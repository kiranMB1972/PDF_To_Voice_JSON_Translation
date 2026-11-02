package com.org.pdfplatform.exception;

public class UploadTimeoutExceptionModel extends RuntimeException {

    private final long timeoutMs;

    public UploadTimeoutExceptionModel(long timeoutMs, String fileName) {
        super("Timeout after " + (timeoutMs / 1000) + " s while reading " + fileName);
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() { return timeoutMs; }

    public String getUserGuidance() { return "This PDF took longer than " + (timeoutMs / 60000) + " minutes. Split it into parts of about 20 pages and try again."; }

    public String getWorkaround() { return "Split the file at https://www.ilovepdf.com/split_pdf"; }

    public String getFyiLink() { return "https://docs.konghq.com/gateway/latest/"; }
}
