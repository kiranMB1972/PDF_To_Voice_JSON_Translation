package com.org.pdfplatform.exception;

/** The translation engine (Ollama) is not reachable or has no such model. The app never fakes a translation. */
public class TranslationUnavailableException extends RuntimeException {

    private final String guidance;

    public TranslationUnavailableException(String message, String guidance, Throwable cause) {
        super(message, cause);
        this.guidance = guidance;
    }

    public String getGuidance() { return guidance; }
}
