package com.org.pdfplatform.exception;

/** A school-module failure with its own HTTP status and visual error code (login failed, database offline, test closed ...). */
public class SchoolException extends RuntimeException {

    private final ErrorCodeModel code;
    private final int status;
    private final String guidance;

    public SchoolException(ErrorCodeModel code, int status, String message, String guidance) {
        super(message);
        this.code = code;
        this.status = status;
        this.guidance = guidance;
    }

    public ErrorCodeModel getCode() { return code; }

    public int getStatus() { return status; }

    public String getGuidance() { return guidance; }
}
