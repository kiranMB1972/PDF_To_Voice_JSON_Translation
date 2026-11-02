package com.org.pdfplatform.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** Replaces the black-and-white Whitelabel page: /error answers a browser with the animated page, anything else with JSON. */
@RestController
public class VisualErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<?> error(HttpServletRequest req) {
        Object s = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = s instanceof Integer i ? i : 500;
        Object p = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = p == null ? req.getRequestURI() : p.toString();
        Object raw = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        ErrorCodeModel code = ErrorGuide.codeForStatus(status);
        String message = null;
        String guidance = null;
        if (raw instanceof Throwable t) {
            ErrorMapper.Mapped m = ErrorMapper.map(t, path);
            status = m.status();
            code = m.code();
            message = m.message();
            guidance = m.guidance();
        }
        HttpStatus http = HttpStatus.resolve(status) == null ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.valueOf(status);
        if (ErrorPageSupport.wantsHtml(req)) {
            String html = ErrorPageRenderer.render(ErrorPageSupport.model(req, status, code, message, guidance, path));
            return ResponseEntity.status(http).contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
        }
        return ResponseEntity.status(http).contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorResponseModel(code, message == null ? ErrorGuide.forCode(code).headline() : message, guidance, null, null));
    }
}
