package com.org.pdfplatform.school.web;

import com.org.pdfplatform.school.model.SchoolUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/** Login state kept in the server session (no extra security framework, so the existing PDF pages stay open as before). */
@Component
public class SchoolSession {

    static final String ATTR_UID = "school.uid";
    static final String ATTR_CSRF = "school.csrf";
    static final String REQ_USER = "school.user";

    private final SecureRandom rnd = new SecureRandom();

    /** Starts a fresh session (prevents session fixation) and returns the CSRF token the page must send with every change. */
    public String login(HttpServletRequest req, SchoolUser u) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession s = req.getSession(true);
        byte[] b = new byte[24];
        rnd.nextBytes(b);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        s.setAttribute(ATTR_UID, u.getId());
        s.setAttribute(ATTR_CSRF, csrf);
        s.setMaxInactiveInterval(2 * 60 * 60);
        return csrf;
    }

    public Long userId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object v = s == null ? null : s.getAttribute(ATTR_UID);
        return v instanceof Long l ? l : null;
    }

    public String csrf(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object v = s == null ? null : s.getAttribute(ATTR_CSRF);
        return v == null ? null : v.toString();
    }

    public void logout(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s != null) {
            s.invalidate();
        }
    }

    /** The logged-in user the interceptor resolved for this request. */
    public static SchoolUser user(HttpServletRequest req) {
        return (SchoolUser) req.getAttribute(REQ_USER);
    }
}
