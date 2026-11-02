package com.org.pdfplatform.school.web;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.ErrorResponseModel;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** A refused value ("Class must be 1 to 10") becomes the visual VALIDATION error instead of a 500. */
@RestControllerAdvice(basePackages = "com.org.pdfplatform.school.web")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchoolExceptionAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseModel> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponseModel(ErrorCodeModel.VALIDATION, e.getMessage(), null, null, null));
    }
}
