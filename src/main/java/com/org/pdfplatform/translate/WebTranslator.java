package com.org.pdfplatform.translate;

/**
 * A free online translator used when the local Ollama engine is not running, so the page can still be translated
 * (Kannada, Tamil, Telugu, Malayalam, Urdu, Hindi, Marathi, Spanish, French, English) without installing anything.
 */
public interface WebTranslator {

    /** False when switched off with translate.web.enabled=false (the text then never leaves this computer). */
    boolean isEnabled();

    /** Short name shown on the page, e.g. "online-translator". */
    String name();

    /**
     * Translates ONE small chunk of text.
     *
     * @param sourceCode a language code such as "kn", or null / "auto" to let the service detect it
     * @param targetCode a language code such as "ta"
     * @throws com.org.pdfplatform.exception.TranslationUnavailableException when no online translator answered
     */
    String translate(String text, String sourceCode, String targetCode);

    /** One line per online route with OK / FAILED and the reason (used by the self-test button). */
    default java.util.List<String> diagnose(String sampleText, String targetCode) {
        return java.util.List.of();
    }
}
