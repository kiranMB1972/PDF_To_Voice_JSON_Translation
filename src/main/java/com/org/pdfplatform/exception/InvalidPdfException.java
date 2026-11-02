package com.org.pdfplatform.exception;

/** A problem with the uploaded file itself (empty, not a PDF, password protected, too many pages). */
public class InvalidPdfException extends RuntimeException {

    private final ErrorCodeModel code;
    private final String guidance;

    public InvalidPdfException(ErrorCodeModel code, String message, String guidance, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.guidance = guidance;
    }

    public InvalidPdfException(ErrorCodeModel code, String message, String guidance) {
        this(code, message, guidance, null);
    }

    public ErrorCodeModel getCode() { return code; }

    public String getGuidance() { return guidance; }
}
