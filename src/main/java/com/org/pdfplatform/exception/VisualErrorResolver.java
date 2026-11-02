package com.org.pdfplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * When a person opens an address in the browser and it fails, show the HD animated error page. Requests made by the page's
 * own buttons (fetch / XHR) are left to GlobalExceptionHandlerModel, which answers with JSON that the page shows as a notice.
 */
@Component
public class VisualErrorResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(VisualErrorResolver.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!ErrorPageSupport.wantsHtml(request) || response.isCommitted()) {
            return null;
        }
        String path = request.getRequestURI();
        ErrorMapper.Mapped m = ErrorMapper.map(ex, path);
        if (m.status() >= 500) {
            log.error("Unexpected error on {} {}", request.getMethod(), path, ex);
        }
        ErrorPageRenderer.PageModel pm = ErrorPageSupport.model(request, m.status(), m.code(), m.message(), m.guidance(), path);
        try {
            response.setStatus(m.status());
            response.setContentType("text/html;charset=UTF-8");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(ErrorPageRenderer.render(pm));
        } catch (IOException e) {
            return null;
        }
        return new ModelAndView();
    }
}
