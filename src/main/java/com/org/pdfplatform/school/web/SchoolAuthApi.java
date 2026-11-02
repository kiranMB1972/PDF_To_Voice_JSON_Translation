package com.org.pdfplatform.school.web;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.service.SchoolService;
import com.org.pdfplatform.school.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/school/api/auth")
public class SchoolAuthApi {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final SchoolUserRepository users;
    private final PasswordEncoder encoder;
    private final SchoolSession session;
    private final SchoolService school;
    private final UserService userService;
    private final Map<String, List<Long>> failures = new ConcurrentHashMap<>();

    public SchoolAuthApi(SchoolUserRepository users, PasswordEncoder encoder, SchoolSession session, SchoolService school, UserService userService) {
        this.users = users;
        this.encoder = encoder;
        this.session = session;
        this.school = school;
        this.userService = userService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String loginAs = body.getOrDefault("loginAs", "").toUpperCase();
        String key = username.toLowerCase();
        if (tooMany(key)) {
            throw new SchoolException(ErrorCodeModel.TOO_MANY_ATTEMPTS, 429, "Too many wrong passwords for '" + username + "'", null);
        }
        SchoolUser u = users.findByUsernameIgnoreCase(username).orElse(null);
        if (u == null || !encoder.matches(password, u.getPasswordHash())) {
            fail(key);
            throw new SchoolException(ErrorCodeModel.LOGIN_FAILED, 401, "The user name or password is not correct", null);
        }
        if (!u.isActive()) {
            throw new SchoolException(ErrorCodeModel.ACCOUNT_DISABLED, 403, "This account is switched off", null);
        }
        if (!loginAs.isEmpty() && !loginAs.equals(u.getRole().name())) {
            throw new SchoolException(ErrorCodeModel.WRONG_PORTAL, 403, "This is a " + u.getRole().name() + " account, but you used the " + loginAs + " login",
                    "Use the " + u.getRole().name() + " login instead.");
        }
        failures.remove(key);
        String csrf = session.login(req, u);
        return me(u, csrf);
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest req) {
        SchoolUser u = SchoolSession.user(req);
        if (u == null) {
            return Views.m("loggedIn", false, "schoolName", school.name());
        }
        return me(u, session.csrf(req));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest req) {
        session.logout(req);
        return Views.m("ok", true);
    }

    @PostMapping("/change-password")
    public Map<String, Object> change(@RequestBody Map<String, String> body, HttpServletRequest req) {
        SchoolUser u = SchoolSession.user(req);
        userService.changePassword(u, body.get("oldPassword"), body.get("newPassword"));
        return Views.m("ok", true);
    }

    private Map<String, Object> me(SchoolUser u, String csrf) {
        return Views.m("loggedIn", true, "role", u.getRole().name(), "name", u.getFullName() == null ? u.getUsername() : u.getFullName(),
                "username", u.getUsername(), "classLevel", u.getClassLevel(), "mustChangePassword", u.isMustChangePassword(), "csrf", csrf,
                "schoolName", school.name());
    }

    private boolean tooMany(String key) {
        List<Long> l = failures.get(key);
        if (l == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (l) {
            l.removeIf(t -> now - t > WINDOW_MS);
            return l.size() >= MAX_FAILURES;
        }
    }

    private void fail(String key) {
        failures.computeIfAbsent(key, k -> new ArrayList<>());
        List<Long> l = failures.get(key);
        synchronized (l) {
            l.add(System.currentTimeMillis());
        }
    }
}
