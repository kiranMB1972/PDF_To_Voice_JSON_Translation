package com.org.pdfplatform.translate;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.RequestRejectedException;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import com.org.pdfplatform.translate.PromptBuilder.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TranslationServiceTest {

    private OllamaClient ollama;
    private TranslationService svc;

    @BeforeEach
    void setUp() {
        ollama = mock(OllamaClient.class);
        when(ollama.getModel()).thenReturn("llama3.1");
        svc = new TranslationService(ollama, new PromptBuilder(), 60000);
    }

    @Test
    void translatesThroughTheEngineAndCleansTheAnswer() {
        when(ollama.generate(anyString())).thenReturn("```\n\u0CB6\u0CC1\u0CAD\u0CA6\u0CBF\u0CA8\n```");

        var r = svc.translate("Good morning.", AppLanguage.KANNADA, RegionalAccent.MYSURU, Mode.TRANSLATE, null);

        assertEquals("ollama:llama3.1", r.engine());
        assertFalse(r.sameLanguage());
        assertEquals("en", r.sourceLanguage());
        assertFalse(r.text().contains("```"));
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ollama).generate(prompt.capture());
        assertTrue(prompt.getValue().contains("Good morning."));
    }

    @Test
    void sameLanguageIsNotSentToTheEngineAtAll() {
        var r = svc.translate("Hello there", AppLanguage.ENGLISH, null, Mode.TRANSLATE, null);
        assertTrue(r.sameLanguage());
        assertEquals("Hello there", r.text());
        verifyNoInteractions(ollama);
    }

    @Test
    void whenTheEngineIsDownTheServiceThrowsInsteadOfInventingATranslation() {
        when(ollama.generate(anyString())).thenThrow(new TranslationUnavailableException("not running", "start it", null));
        assertThrows(TranslationUnavailableException.class,
                () -> svc.translate("Hello", AppLanguage.TAMIL, RegionalAccent.STANDARD, Mode.TRANSLATE, null));
    }

    @Test
    void blankAndOversizedTextAreRejectedWithClearCodes() {
        assertEquals(ErrorCodeModel.EMPTY_TEXT, assertThrows(RequestRejectedException.class,
                () -> svc.translate("  ", AppLanguage.TAMIL, null, Mode.TRANSLATE, null)).getCode());
        TranslationService small = new TranslationService(ollama, new PromptBuilder(), 10);
        assertEquals(ErrorCodeModel.TEXT_TOO_LONG, assertThrows(RequestRejectedException.class,
                () -> small.translate("This text is longer than ten characters", AppLanguage.TAMIL, null, Mode.TRANSLATE, null)).getCode());
    }

    @Test
    void longPagesAreSplitIntoChunksAndJoinedInOrder() {
        when(ollama.generate(anyString())).thenReturn("\u0C05", "\u0C06", "\u0C07", "\u0C08", "\u0C09", "\u0C0A");
        String para = "This is a sentence of reasonable length. ".repeat(30);        // ~1200 chars
        String text = para + "\n\n" + para + "\n\n" + para;

        var r = svc.translate(text, AppLanguage.TELUGU, null, Mode.ORATE, "en");

        assertTrue(r.chunks() >= 2);
        assertTrue(r.text().startsWith("\u0C05\n\n\u0C06"));
        verify(ollama, times(r.chunks())).generate(anyString());
    }

    @Test
    void chunkNeverExceedsTheLimitAndKeepsAllWords() {
        String text = ("word".repeat(5) + " ").repeat(500);                          // ~10 500 chars, no punctuation
        List<String> chunks = TranslationService.chunk(text, 1800);
        assertTrue(chunks.size() >= 6);
        assertTrue(chunks.stream().allMatch(c -> c.length() <= 1800));
        assertEquals(text.replaceAll("\\s+", " ").strip().length(),
                String.join(" ", chunks).replaceAll("\\s+", " ").strip().length());
    }

    @Test
    void sourceLanguageIsDetectedFromTheScript() {
        when(ollama.generate(anyString())).thenReturn("Hello");
        var r = svc.translate("\u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0 \u0CB6\u0CBE\u0CB2\u0CC6", AppLanguage.ENGLISH, null, Mode.TRANSLATE, null);
        assertEquals("kn", r.sourceLanguage());
        assertEquals("Hello", r.text());
    }

    // ---- online translator fallback -------------------------------------------------------------------------

    private static WebTranslator web(boolean enabled, java.util.function.UnaryOperator<String> fn) {
        return new WebTranslator() {
            public boolean isEnabled() { return enabled; }
            public String name() { return "online-translator"; }
            public String translate(String t, String s, String target) { return fn.apply(t) + "[" + target + "]"; }
        };
    }

    @Test
    void whenOllamaIsNotRunningTheOnlineTranslatorIsUsedInstead() {
        when(ollama.isAvailable()).thenReturn(false);
        var both = new TranslationService(ollama, web(true, t -> "T"), new PromptBuilder(), 60000, "auto");

        var r = both.translate("Good morning.", AppLanguage.KANNADA, RegionalAccent.MYSURU, Mode.TRANSLATE, null);

        assertEquals("online-translator", r.engine());
        assertEquals("T[kn]", r.text());
        verify(ollama, never()).generate(anyString());
    }

    @Test
    void ollamaIsPreferredWhenItIsRunning() {
        when(ollama.isAvailable()).thenReturn(true);
        when(ollama.generate(anyString())).thenReturn("\u0928\u092E\u0938\u094D\u0924\u0947");
        var both = new TranslationService(ollama, web(true, t -> "T"), new PromptBuilder(), 60000, "auto");
        assertEquals("ollama:llama3.1", both.translate("Hello", AppLanguage.HINDI, null, Mode.TRANSLATE, null).engine());
    }

    @Test
    void ollamaUpButBrokenFallsBackToTheOnlineTranslator() {
        when(ollama.isAvailable()).thenReturn(true);
        when(ollama.generate(anyString())).thenThrow(new TranslationUnavailableException("model missing", "ollama pull", null));
        var both = new TranslationService(ollama, web(true, t -> "T"), new PromptBuilder(), 60000, "auto");
        assertEquals("online-translator", both.translate("Hello", AppLanguage.MARATHI, null, Mode.TRANSLATE, null).engine());
    }

    @Test
    void preferWebSkipsOllamaAndDisabledWebKeepsTheOldBehaviour() {
        var webFirst = new TranslationService(ollama, web(true, t -> "T"), new PromptBuilder(), 60000, "web");
        assertEquals("online-translator", webFirst.translate("Hello", AppLanguage.FRENCH, null, Mode.TRANSLATE, null).engine());
        verify(ollama, never()).isAvailable();

        var off = new TranslationService(ollama, web(false, t -> "T"), new PromptBuilder(), 60000, "auto");
        when(ollama.generate(anyString())).thenThrow(new TranslationUnavailableException("not running", "start it", null));
        assertThrows(TranslationUnavailableException.class, () -> off.translate("Hello", AppLanguage.TAMIL, null, Mode.TRANSLATE, null));
    }

    @Test
    void whenBothEnginesFailTheErrorMentionsBoth() {
        when(ollama.isAvailable()).thenReturn(true);
        when(ollama.generate(anyString())).thenThrow(new TranslationUnavailableException("model missing", "pull", null));
        WebTranslator broken = new WebTranslator() {
            public boolean isEnabled() { return true; }
            public String name() { return "x"; }
            public String translate(String t, String s, String target) { throw new TranslationUnavailableException("no internet", "connect", null); }
        };
        var both = new TranslationService(ollama, broken, new PromptBuilder(), 60000, "auto");
        var e = assertThrows(TranslationUnavailableException.class, () -> both.translate("Hi", AppLanguage.URDU, null, Mode.TRANSLATE, null));
        assertTrue(e.getMessage().contains("model missing") && e.getMessage().contains("no internet"));
    }

    // ---- the local model must really answer in the target language ----------------------------------------

    @Test
    void anEnglishAnswerForHindiIsNotShownAsHindiAndTheOnlineTranslatorTakesOver() {
        when(ollama.isAvailable()).thenReturn(true);
        when(ollama.generate(anyString())).thenReturn("Good morning children.");           // the model ignored the instruction
        var both = new TranslationService(ollama, web(true, t -> "T"), new PromptBuilder(), 60000, "auto");

        var r = both.translate("Good morning children.", AppLanguage.HINDI, null, Mode.TRANSLATE, null);

        assertEquals("online-translator", r.engine());
    }

    @Test
    void withoutTheOnlineTranslatorAWrongLanguageAnswerIsAnErrorNotAFakeTranslation() {
        when(ollama.generate(anyString())).thenReturn("Namaste bachchon");                // transliteration, not Devanagari
        var e = assertThrows(TranslationUnavailableException.class,
                () -> svc.translate("Hello children", AppLanguage.MARATHI, null, Mode.TRANSLATE, null));
        assertTrue(e.getMessage().contains("Marathi"));
    }

    @Test
    void spanishAndFrenchAnswersThatDifferFromTheInputAreAccepted() {
        when(ollama.generate(anyString())).thenReturn("Buenos d\u00EDas, ni\u00F1os.", "Bonjour les enfants.");
        assertEquals("Buenos d\u00EDas, ni\u00F1os.", svc.translate("Good morning, children.", AppLanguage.SPANISH, null, Mode.TRANSLATE, null).text());
        assertEquals("Bonjour les enfants.", svc.translate("Hello children.", AppLanguage.FRENCH, null, Mode.TRANSLATE, null).text());
    }
}
