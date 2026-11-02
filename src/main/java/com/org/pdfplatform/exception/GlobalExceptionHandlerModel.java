package com.org.pdfplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Every failure leaves the API as a small JSON object the page can show as a notice. */
@RestControllerAdvice
public class GlobalExceptionHandlerModel {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlerModel.class);

    @ExceptionHandler(UploadTimeoutExceptionModel.class)
    public ResponseEntity<ErrorResponseModel> handleTimeout(UploadTimeoutExceptionModel ex) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(new ErrorResponseModel(
                ErrorCodeModel.TIMEOUT, ex.getMessage(), ex.getUserGuidance(), ex.getWorkaround(), ex.getFyiLink()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseModel> handleMax(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(new ErrorResponseModel(
                ErrorCodeModel.FILE_TOO_LARGE, "File too large",
                "The limit is 50 MB. Compress the PDF or split it into parts.",
                "Split the file at https://www.ilovepdf.com/split_pdf",
                "https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.multipart"));
    }

    @ExceptionHandler(InvalidPdfException.class)
    public ResponseEntity<ErrorResponseModel> handleInvalid(InvalidPdfException ex) {
        HttpStatus status = ex.getCode() == ErrorCodeModel.TOO_MANY_PAGES ? HttpStatus.PAYLOAD_TOO_LARGE
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(new ErrorResponseModel(
                ex.getCode(), ex.getMessage(), ex.getGuidance(), null, "https://pdfbox.apache.org/"));
    }

    @ExceptionHandler(TranslationUnavailableException.class)
    public ResponseEntity<ErrorResponseModel> handleTranslationOffline(TranslationUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponseModel(
                ErrorCodeModel.TRANSLATION_ENGINE_OFFLINE, ex.getMessage(), ex.getGuidance(), null,
                "https://ollama.com/download"));
    }

    @ExceptionHandler(RequestRejectedException.class)
    public ResponseEntity<ErrorResponseModel> handleRejected(RequestRejectedException ex) {
        HttpStatus status = ex.getCode() == ErrorCodeModel.TEXT_TOO_LONG ? HttpStatus.PAYLOAD_TOO_LARGE : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ErrorResponseModel(ex.getCode(), ex.getMessage(), ex.getGuidance(), null, null));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponseModel> handleMissingPart(MissingServletRequestPartException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponseModel(
                ErrorCodeModel.MISSING_FILE, "No PDF was attached", "Choose a PDF file, then press Analyze.", null, null));
    }

    /** Wrong method, unknown address, missing value, school failures: one consistent JSON answer instead of a generic 500. */
    @ExceptionHandler({SchoolException.class, HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class,
            NoResourceFoundException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponseModel> handleFramework(Exception ex, HttpServletRequest request) {
        ErrorMapper.Mapped m = ErrorMapper.map(ex, request.getRequestURI());
        return ResponseEntity.status(m.status()).body(new ErrorResponseModel(m.code(), m.message(), m.guidance(), null, null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseModel> handleAny(Exception ex) {
        log.error("Unexpected error", ex);                       // full detail stays in the server log
        return ResponseEntity.internalServerError().body(new ErrorResponseModel(
                ErrorCodeModel.INTERNAL_ERROR, "Something went wrong while processing the PDF",
                "Try again. If it keeps happening, try a different PDF.", null, null));
    }
}
