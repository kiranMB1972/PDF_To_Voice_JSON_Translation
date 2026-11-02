package com.org.pdfplatform.exception;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** One place that turns any exception into (HTTP status, visual error code, message, case-specific guidance). */
public final class ErrorMapper {

    public record Mapped(int status, ErrorCodeModel code, String message, String guidance) { }

    private ErrorMapper() { }

    public static Mapped map(Throwable ex, String path) {
        if (ex instanceof SchoolException e) {
            return new Mapped(e.getStatus(), e.getCode(), e.getMessage(), e.getGuidance());
        }
        if (ex instanceof RequestRejectedException e) {
            return new Mapped(e.getCode() == ErrorCodeModel.TEXT_TOO_LONG ? 413 : 400, e.getCode(), e.getMessage(), e.getGuidance());
        }
        if (ex instanceof InvalidPdfException e) {
            return new Mapped(e.getCode() == ErrorCodeModel.TOO_MANY_PAGES ? 413 : 422, e.getCode(), e.getMessage(), e.getGuidance());
        }
        if (ex instanceof UploadTimeoutExceptionModel e) {
            return new Mapped(408, ErrorCodeModel.TIMEOUT, e.getMessage(), e.getUserGuidance());
        }
        if (ex instanceof TranslationUnavailableException e) {
            return new Mapped(503, ErrorCodeModel.TRANSLATION_ENGINE_OFFLINE, e.getMessage(), e.getGuidance());
        }
        if (ex instanceof MaxUploadSizeExceededException) {
            return new Mapped(413, ErrorCodeModel.FILE_TOO_LARGE, "The file is bigger than the upload limit", "Compress the PDF or split it into parts.");
        }
        if (ex instanceof MissingServletRequestPartException) {
            return new Mapped(400, ErrorCodeModel.MISSING_FILE, "No PDF was attached", "Choose a PDF file, then press Analyze.");
        }
        if (ex instanceof HttpRequestMethodNotSupportedException e) {
            return new Mapped(405, ErrorCodeModel.METHOD_NOT_ALLOWED, "This address does not accept " + e.getMethod(), "Supported: " + String.join(", ", e.getSupportedMethods() == null ? new String[0] : e.getSupportedMethods()));
        }
        if (ex instanceof HttpMediaTypeNotSupportedException) {
            return new Mapped(415, ErrorCodeModel.UNSUPPORTED_MEDIA, "The server cannot read this kind of content", null);
        }
        if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
            return new Mapped(404, ErrorCodeModel.NOT_FOUND, "Nothing exists at " + (path == null ? "this address" : path), null);
        }
        if (ex instanceof MissingServletRequestParameterException e) {
            return new Mapped(400, ErrorCodeModel.BAD_REQUEST, "The value '" + e.getParameterName() + "' is missing", "Fill in '" + e.getParameterName() + "' and try again.");
        }
        if (ex instanceof MethodArgumentTypeMismatchException e) {
            return new Mapped(400, ErrorCodeModel.BAD_REQUEST, "'" + e.getName() + "' has a value of the wrong type", "Check the value you entered for '" + e.getName() + "'.");
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return new Mapped(400, ErrorCodeModel.BAD_REQUEST, "The data sent to the server could not be read", "Reload the page and try again.");
        }
        if (ex instanceof IllegalArgumentException e && path != null && path.startsWith("/school")) {
            return new Mapped(400, ErrorCodeModel.VALIDATION, e.getMessage(), null);
        }
        return new Mapped(500, ErrorCodeModel.INTERNAL_ERROR, "Something went wrong while processing the request", null);
    }
}
