package com.org.pdfplatform.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerModelTest {

    private final GlobalExceptionHandlerModel h = new GlobalExceptionHandlerModel();

    @Test
    void oversizedUploadGives413WithTheFileTooLargeCode() {
        var r = h.handleMax(new MaxUploadSizeExceededException(50L * 1024 * 1024));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, r.getStatusCode());
        assertEquals(ErrorCodeModel.FILE_TOO_LARGE, r.getBody().code());
    }

    @Test
    void timeoutGives408WithSplitAdvice() {
        var r = h.handleTimeout(new UploadTimeoutExceptionModel(300000, "big.pdf"));
        assertEquals(HttpStatus.REQUEST_TIMEOUT, r.getStatusCode());
        assertTrue(r.getBody().userGuidance().contains("5 minutes"));
        assertTrue(r.getBody().error().contains("big.pdf"));
    }

    @Test
    void tooManyPagesIs413AndOtherInvalidPdfIs422() {
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                h.handleInvalid(new InvalidPdfException(ErrorCodeModel.TOO_MANY_PAGES, "m", "g")).getStatusCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY,
                h.handleInvalid(new InvalidPdfException(ErrorCodeModel.PASSWORD_PROTECTED, "m", "g")).getStatusCode());
    }
}
