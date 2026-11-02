package com.org.pdfplatform.school.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.ErrorPageRenderer;
import com.org.pdfplatform.exception.ErrorPageSupport;
import com.org.pdfplatform.exception.ErrorResponseModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.service.SchoolDb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Guards /school/api/** and /school/files/**: the database must be ready, the person must be logged in, admin and student
 * areas are separate, and every change must carry the session's CSRF token.
 */
@Component
public class SchoolAuthInterceptor implements HandlerInterceptor {

    private final SchoolDb db;
    private final SchoolSession session;
    private final SchoolUserRepository users;
    private final ObjectMapper mapper;

    public SchoolAuthInterceptor(SchoolDb db, SchoolSession session, SchoolUserRepository users, ObjectMapper mapper) {
        this.db = db;
        this.session = session;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        try {
            check(request);
            return true;
        } catch (SchoolException e) {
            write(request, response, e);
            return false;
        }
    }

    private void check(HttpServletRequest request) {
        db.ensureReady();
        String path = request.getRequestURI();
        boolean open = path.equals("/school/api/auth/login") || path.equals("/school/api/auth/me") || path.equals("/school/api/auth/logout");
        Long uid = session.userId(request);
        SchoolUser u = uid == null ? null : users.findById(uid).filter(SchoolUser::isActive).orElse(null);
        request.setAttribute(SchoolSession.REQ_USER, u);
        if (open) {
            return;
        }
        if (u == null) {
            throw new SchoolException(ErrorCodeModel.AUTH_REQUIRED, 401, "You are not logged in (or the session ended)", null);
        }
        if (path.startsWith("/school/api/admin") && u.getRole() != Role.ADMIN) {
            throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "This area is for the school admin only", null);
        }
        if (path.startsWith("/school/api/student") && u.getRole() != Role.STUDENT) {
            throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "This area is for students only", null);
        }
        String m = request.getMethod();
        if (!(m.equals("GET") || m.equals("HEAD") || m.equals("OPTIONS"))) {
            String token = request.getHeader("X-CSRF-Token");
            String expected = session.csrf(request);
            if (token == null || expected == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "The security token is missing or old", "Reload the School screen and try again.");
            }
        }
    }

    private void write(HttpServletRequest request, HttpServletResponse response, SchoolException e) throws IOException {
        response.setStatus(e.getStatus());
        response.setCharacterEncoding("UTF-8");
        if (ErrorPageSupport.wantsHtml(request)) {
            response.setContentType("text/html;charset=UTF-8");
            ErrorPageRenderer.PageModel pm = ErrorPageSupport.model(request, e.getStatus(), e.getCode(), e.getMessage(), e.getGuidance(), request.getRequestURI());
            response.getWriter().write(ErrorPageRenderer.render(pm));
        } else {
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), new ErrorResponseModel(e.getCode(), e.getMessage(), e.getGuidance(), null, null));
        }
    }
}
