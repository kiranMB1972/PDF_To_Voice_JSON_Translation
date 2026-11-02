package com.org.pdfplatform.exception;

/** The request itself is wrong (blank text, unknown language ...). */
public class RequestRejectedException extends RuntimeException {

    private final ErrorCodeModel code;
    private final String guidance;

    public RequestRejectedException(ErrorCodeModel code, String message, String guidance) {
        super(message);
        this.code = code;
        this.guidance = guidance;
    }

    public ErrorCodeModel getCode() { return code; }

    public String getGuidance() { return guidance; }
}
