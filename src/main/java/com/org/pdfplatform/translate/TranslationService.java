package com.org.pdfplatform.translate;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.RequestRejectedException;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import com.org.pdfplatform.translate.PromptBuilder.Mode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates page text. Engine order (translate.prefer=auto): the local Ollama model when it is running, otherwise the
 * free online translator, so a translation is produced even on a PC without Ollama. Honest by design: if NO engine
 * works it throws (the page then reads the ORIGINAL text) - it never invents "translations" made of labels and English.
 * Accent wording and the Orate/Teach modes are only possible with Ollama; the online translator translates faithfully.
 */
@Service
public class TranslationService {

    public record Result(String text, String engine, int chunks, boolean sameLanguage, String sourceLanguage) { }

    private static final int WEB_CHUNK = 1200;

    private final OllamaClient ollama;
    private final WebTranslator web;                 // null = no online fallback
    private final PromptBuilder prompts;
    private final int maxTextChars;
    private final String prefer;

    /** Ollama only (no online fallback). */
    public TranslationService(OllamaClient ollama, PromptBuilder prompts, int maxTextChars) {
        this(ollama, null, prompts, maxTextChars, "auto");
    }

    @Autowired
    public TranslationService(OllamaClient ollama, WebTranslator web, PromptBuilder prompts,
                              @Value("${translate.max-text-chars:60000}") int maxTextChars,
                              @Value("${translate.prefer:auto}") String prefer) {
        this.ollama = ollama;
        this.web = web;
        this.prompts = prompts;
        this.maxTextChars = maxTextChars;
        this.prefer = prefer == null ? "auto" : prefer.trim().toLowerCase();
    }

    public boolean isWebEnabled() { return web != null && web.isEnabled(); }

    public Result translate(String text, AppLanguage target, RegionalAccent accent, Mode mode, String sourceCodeOrNull) {
        if (text == null || text.isBlank()) {
            throw new RequestRejectedException(ErrorCodeModel.EMPTY_TEXT, "There is no text to translate",
                    "This page has no readable text (picture or scan).");
        }
        if (text.length() > maxTextChars) {
            throw new RequestRejectedException(ErrorCodeModel.TEXT_TOO_LONG,
                    "The text is " + text.length() + " characters (limit " + maxTextChars + ")",
                    "Translate one page at a time.");
        }
        String source = (sourceCodeOrNull == null || sourceCodeOrNull.isBlank()) ? ScriptDetector.detect(text) : sourceCodeOrNull;
        AppLanguage sourceLang = AppLanguage.find(source).orElse(null);        // null = a language that is not in the list
        if (sourceLang == target) {
            return new Result(text, "none", 0, true, source);
        }
        boolean webOn = isWebEnabled();
        boolean ollamaFirst = !(webOn && "web".equals(prefer));
        TranslationUnavailableException ollamaProblem = null;
        if (ollamaFirst && (!webOn || ollama.isAvailable())) {         // no fallback -> call Ollama directly so its precise error shows
            try {
                return viaOllama(text, target, accent, mode, source, sourceLang);
            } catch (TranslationUnavailableException e) {
                if (!webOn) {
                    throw e;
                }
                ollamaProblem = e;                                      // Ollama is up but failed (e.g. model missing): use the online translator
            }
        }
        try {
            return viaWeb(text, target, source);
        } catch (TranslationUnavailableException e) {
            if (ollamaProblem == null) {
                throw e;
            }
            throw new TranslationUnavailableException("No translation engine worked. Ollama: " + ollamaProblem.getMessage()
                    + ". Online translator: " + e.getMessage(), e.getGuidance(), e);
        }
    }

    private Result viaOllama(String text, AppLanguage target, RegionalAccent accent, Mode mode, String source, AppLanguage sourceLang) {
        String sourceName = sourceLang == null ? "the original language of the text" : sourceLang.getEnglishName();
        List<String> chunks = chunk(text, 1800);
        List<String> out = new ArrayList<>(chunks.size());
        for (String c : chunks) {
            String raw = ollama.generate(prompts.build(c, target, accent == null ? RegionalAccent.STANDARD : accent,
                    mode == null ? Mode.TRANSLATE : mode, sourceName));
            out.add(prompts.clean(raw));
        }
        String joined = String.join("\n\n", out);
        if (!ScriptDetector.outputMatchesTarget(text, joined, target.getCode())) {
            throw new TranslationUnavailableException("The local model " + ollama.getModel() + " did not return " + target.getEnglishName()
                    + " text (it answered in another language or repeated the original)",
                    "A larger model translates these languages better (ollama pull llama3.1:70b / qwen2.5:14b), or use the online translator.", null);
        }
        return new Result(joined, "ollama:" + ollama.getModel(), chunks.size(), false, source);
    }

    /** For the page's "Test translation" button: shows exactly which engine answered or why each one failed. */
    public java.util.Map<String, Object> selfTest(AppLanguage target) {
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        String sample = "Good morning. Today we will learn about trees and how they help us.";
        m.put("target", target.getCode());
        m.put("ollamaRunning", ollama.isAvailable());
        m.put("onlineEnabled", isWebEnabled());
        m.put("online", isWebEnabled() ? web.diagnose(sample, target.getCode()) : java.util.List.of("switched off (translate.web.enabled=false)"));
        try {
            Result r = translate(sample, target, RegionalAccent.STANDARD, Mode.TRANSLATE, "en");
            m.put("result", r.text());
            m.put("engine", r.engine());
            m.put("ok", true);
        } catch (RuntimeException e) {
            m.put("ok", false);
            m.put("error", e.getMessage());
        }
        return m;
    }

    private Result viaWeb(String text, AppLanguage target, String source) {
        List<String> chunks = chunk(text, WEB_CHUNK);
        List<String> out = new ArrayList<>(chunks.size());
        for (String c : chunks) {
            out.add(web.translate(c, "auto", target.getCode()));        // "auto": Latin text may be Spanish/French, not only English
        }
        return new Result(String.join("\n\n", out), web.name(), chunks.size(), false, source);
    }

    /** Splits at blank lines, then sentence ends, so no chunk is bigger than max and no sentence is cut mid-way. */
    static List<String> chunk(String text, int max) {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String para : text.split("\\n\\s*\\n")) {
            String p = para.strip();
            if (p.isEmpty()) {
                continue;
            }
            if (p.length() > max) {
                flush(chunks, cur);
                String[] sentences = p.split("(?<=[.!?\\u0964\\u0965\\u06D4\\u061F])\\s+");
                for (String s : sentences) {
                    while (s.length() > max) {                        // a "sentence" with no punctuation at all
                        int cut = s.lastIndexOf(' ', max);
                        if (cut < max / 3) {
                            cut = max;
                        }
                        append(chunks, cur, s.substring(0, cut), max);
                        s = s.substring(cut).strip();
                    }
                    append(chunks, cur, s, max);
                }
            } else {
                append(chunks, cur, p, max);
            }
        }
        flush(chunks, cur);
        return chunks.isEmpty() ? List.of(text.strip()) : chunks;
    }

    private static void append(List<String> chunks, StringBuilder cur, String piece, int max) {
        if (piece.isEmpty()) {
            return;
        }
        if (cur.length() > 0 && cur.length() + 2 + piece.length() > max) {
            flush(chunks, cur);
        }
        if (cur.length() > 0) {
            cur.append("\n\n");
        }
        cur.append(piece);
    }

    private static void flush(List<String> chunks, StringBuilder cur) {
        if (cur.length() > 0) {
            chunks.add(cur.toString());
            cur.setLength(0);
        }
    }
}
