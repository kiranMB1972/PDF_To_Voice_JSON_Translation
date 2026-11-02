package com.org.pdfplatform.controller;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.RequestRejectedException;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import com.org.pdfplatform.translate.OllamaClient;
import com.org.pdfplatform.translate.PromptBuilder;
import com.org.pdfplatform.translate.TranslationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Page-at-a-time translation. The page asks for the page it is showing (plus the next one while reading). */
@RestController
@RequestMapping("/api/v1/translate")
public class TranslateApiController {

    public record TranslateRequest(String text, String target, String accent, String mode, String source) { }

    public record TranslateResponse(String translation, String engine, int chunks, boolean sameLanguage,
                                    String sourceLanguage, String target) { }

    private final TranslationService service;
    private final OllamaClient ollama;
    private final Executor executor;
    private final long timeoutMs;

    public TranslateApiController(TranslationService service, OllamaClient ollama,
                                  @Qualifier("pdfUploadExecutor") Executor executor,
                                  @Value("${translate.timeout-ms:180000}") long timeoutMs) {
        this.service = service;
        this.ollama = ollama;
        this.executor = executor;
        this.timeoutMs = timeoutMs;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean ollamaUp = ollama.isAvailable();
        boolean online = service.isWebEnabled();
        boolean available = ollamaUp || online;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", available);
        m.put("engine", ollamaUp ? "ollama" : online ? "online" : "none");
        m.put("label", ollamaUp ? "Ollama " + ollama.getModel() : online ? "free online translator" : "offline");
        m.put("model", ollama.getModel());
        m.put("ollama", ollamaUp);
        m.put("online", online);
        m.put("hint", ollamaUp ? "Translation engine is ready (local Ollama)."
                : online ? "Translating with the free online translator (internet needed). For offline use start Ollama:  ollama serve"
                : "Translation engine is offline. Start it with:  ollama serve   then:  ollama pull " + ollama.getModel()
                + "   (free, local). Until then the original text is read aloud.");
        return m;
    }

    @GetMapping("/selftest")
    public Map<String, Object> selfTest(@RequestParam String target) {
        AppLanguage lang = AppLanguage.find(target).orElseThrow(() -> new RequestRejectedException(
                ErrorCodeModel.INVALID_LANGUAGE, "Unknown target language: " + target, "Choose one of the languages in the list."));
        return service.selfTest(lang);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<TranslateResponse>> translate(@RequestBody TranslateRequest req) {
        AppLanguage target = AppLanguage.find(req.target()).orElseThrow(() -> new RequestRejectedException(
                ErrorCodeModel.INVALID_LANGUAGE, "Unknown target language: " + req.target(),
                "Choose one of the languages in the list."));
        RegionalAccent accent = RegionalAccent.fromCode(req.accent());
        PromptBuilder.Mode mode = PromptBuilder.Mode.of(req.mode());
        return CompletableFuture
                .supplyAsync(() -> service.translate(req.text(), target, accent, mode, req.source()), executor)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .handle((r, ex) -> {
                    if (ex == null) {
                        return ResponseEntity.ok(new TranslateResponse(r.text(), r.engine(), r.chunks(), r.sameLanguage(),
                                r.sourceLanguage(), target.getCode()));
                    }
                    Throwable t = ex;
                    while (t instanceof CompletionException && t.getCause() != null) {
                        t = t.getCause();
                    }
                    if (t instanceof TimeoutException) {
                        throw new TranslationUnavailableException("The translation took longer than " + (timeoutMs / 1000) + " seconds",
                                "Use a smaller/faster model (ollama pull llama3.2) or translate shorter pages.", t);
                    }
                    if (t instanceof RuntimeException re) {
                        throw re;
                    }
                    throw new IllegalStateException(t);
                });
    }
}
