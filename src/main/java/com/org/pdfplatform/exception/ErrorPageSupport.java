package com.org.pdfplatform.exception;

import jakarta.servlet.http.HttpServletRequest;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Shared by the /error controller and the exception resolver: "does this request want a page?" and the page's model. */
public final class ErrorPageSupport {

    private static final SecureRandom RND = new SecureRandom();
    private static final String ALPHA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private ErrorPageSupport() { }

    /** A browser opening an address wants HTML; the page's own fetch() calls want JSON. */
    public static boolean wantsHtml(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        if (accept == null || !accept.contains("text/html")) {
            return false;
        }
        if ("XMLHttpRequest".equalsIgnoreCase(req.getHeader("X-Requested-With"))) {
            return false;
        }
        String mode = req.getHeader("Sec-Fetch-Mode");
        return mode == null || !mode.equalsIgnoreCase("cors");
    }

    public static String newRef() {
        StringBuilder sb = new StringBuilder("ERR-").append(DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now())).append('-');
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHA.charAt(RND.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    public static ErrorPageRenderer.PageModel model(HttpServletRequest req, int status, ErrorCodeModel code, String message, String guidance, String path) {
        ErrorGuide.Guide g = ErrorGuide.forCode(code);
        String method = req.getMethod();
        return new ErrorPageRenderer.PageModel(status, g.code(), g, message, guidance, ErrorGuide.operationFor(method, path), method, path,
                newRef(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now()));
    }
}
