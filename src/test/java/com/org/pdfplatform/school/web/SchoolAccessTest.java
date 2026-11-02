package com.org.pdfplatform.school.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.service.MaterialService;
import com.org.pdfplatform.school.service.SchoolDb;
import com.org.pdfplatform.school.service.SchoolService;
import com.org.pdfplatform.school.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchoolAccessTest {

    SchoolUserRepository users;
    PasswordEncoder encoder;
    SchoolSession session;
    SchoolDb db;
    SchoolAuthApi auth;
    SchoolAuthInterceptor gate;
    SchoolUser anita, admin;

    private SchoolUser user(long id, String name, Role role, boolean active) {
        SchoolUser u = new SchoolUser();
        u.setId(id);
        u.setUsername(name);
        u.setFullName(name);
        u.setRole(role);
        u.setActive(active);
        u.setClassLevel(7);
        u.setPasswordHash(encoder.encode("secret1"));
        return u;
    }

    @BeforeEach
    void setUp() {
        users = mock(SchoolUserRepository.class);
        encoder = new BCryptPasswordEncoder();
        session = new SchoolSession();
        db = mock(SchoolDb.class);
        SchoolService school = mock(SchoolService.class);
        when(school.name()).thenReturn("Green Valley School");
        anita = user(5, "anita7a", Role.STUDENT, true);
        admin = user(1, "admin", Role.ADMIN, true);
        when(users.findByUsernameIgnoreCase("anita7a")).thenReturn(Optional.of(anita));
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(users.findById(5L)).thenReturn(Optional.of(anita));
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        auth = new SchoolAuthApi(users, encoder, session, school, mock(UserService.class));
        gate = new SchoolAuthInterceptor(db, session, users, new ObjectMapper());
    }

    private Map<String, String> creds(String u, String p, String as) {
        return Map.of("username", u, "password", p, "loginAs", as);
    }

    @Test
    void aStudentLogsInThroughTheStudentPortalAndGetsACsrfToken() {
        Map<String, Object> r = auth.login(creds("anita7a", "secret1", "STUDENT"), new MockHttpServletRequest());
        assertEquals("STUDENT", r.get("role"));
        assertNotNull(r.get("csrf"));
        assertEquals("Green Valley School", r.get("schoolName"));
    }

    @Test
    void wrongPasswordWrongPortalAndSwitchedOffAccountsGetTheirOwnVisualError() {
        assertEquals(ErrorCodeModel.LOGIN_FAILED, assertThrows(SchoolException.class, () -> auth.login(creds("anita7a", "nope", "STUDENT"), new MockHttpServletRequest())).getCode());
        assertEquals(ErrorCodeModel.WRONG_PORTAL, assertThrows(SchoolException.class, () -> auth.login(creds("anita7a", "secret1", "ADMIN"), new MockHttpServletRequest())).getCode());
        anita.setActive(false);
        assertEquals(ErrorCodeModel.ACCOUNT_DISABLED, assertThrows(SchoolException.class, () -> auth.login(creds("anita7a", "secret1", "STUDENT"), new MockHttpServletRequest())).getCode());
    }

    @Test
    void fiveWrongPasswordsLockTheLoginForAWhile() {
        for (int i = 0; i < 5; i++) {
            assertThrows(SchoolException.class, () -> auth.login(creds("anita7a", "bad", "STUDENT"), new MockHttpServletRequest()));
        }
        SchoolException e = assertThrows(SchoolException.class, () -> auth.login(creds("anita7a", "secret1", "STUDENT"), new MockHttpServletRequest()));
        assertEquals(ErrorCodeModel.TOO_MANY_ATTEMPTS, e.getCode());
        assertEquals(429, e.getStatus());
    }

    private MockHttpServletRequest loggedIn(String method, String path, SchoolUser u) {
        MockHttpServletRequest login = new MockHttpServletRequest();
        String csrf = session.login(login, u);
        MockHttpServletRequest r = new MockHttpServletRequest(method, path);
        r.setRequestURI(path);
        r.setSession(login.getSession());
        r.addHeader("X-CSRF-Token", csrf);
        return r;
    }

    @Test
    void protectedAddressesNeedLoginTheRightRoleAndTheCsrfToken() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockHttpServletRequest anon = new MockHttpServletRequest("GET", "/school/api/student/home");
        anon.setRequestURI("/school/api/student/home");
        assertFalse(gate.preHandle(anon, res, null));
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentAsString().contains("AUTH_REQUIRED"));

        MockHttpServletResponse r2 = new MockHttpServletResponse();
        assertFalse(gate.preHandle(loggedIn("GET", "/school/api/admin/overview", anita), r2, null));
        assertEquals(403, r2.getStatus());                                                 // a student cannot open the admin area

        MockHttpServletResponse r3 = new MockHttpServletResponse();
        MockHttpServletRequest noToken = loggedIn("POST", "/school/api/admin/students", admin);
        MockHttpServletRequest stripped = new MockHttpServletRequest("POST", "/school/api/admin/students");
        stripped.setRequestURI("/school/api/admin/students");
        stripped.setSession(noToken.getSession());
        assertFalse(gate.preHandle(stripped, r3, null));
        assertEquals(403, r3.getStatus());                                                 // a change without the token is refused

        MockHttpServletResponse ok = new MockHttpServletResponse();
        assertTrue(gate.preHandle(loggedIn("POST", "/school/api/admin/students", admin), ok, null));
        assertTrue(gate.preHandle(loggedIn("GET", "/school/api/student/home", anita), new MockHttpServletResponse(), null));
    }

    @Test
    void whenTheDatabaseIsOfflineTheBrowserGetsTheVisualPageAndTheApiGetsJson() throws Exception {
        org.mockito.Mockito.doThrow(new SchoolException(ErrorCodeModel.SCHOOL_DB_OFFLINE, 503, "no route", "retry")).when(db).ensureReady();
        MockHttpServletRequest page = new MockHttpServletRequest("GET", "/school/files/material/1");
        page.setRequestURI("/school/files/material/1");
        page.addHeader("Accept", "text/html");
        MockHttpServletResponse res = new MockHttpServletResponse();
        assertFalse(gate.preHandle(page, res, null));
        assertEquals(503, res.getStatus());
        assertTrue(res.getContentAsString().contains("The school database is not reachable"));
        MockHttpServletRequest api = new MockHttpServletRequest("GET", "/school/api/auth/me");
        api.setRequestURI("/school/api/auth/me");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        assertFalse(gate.preHandle(api, res2, null));
        assertTrue(res2.getContentAsString().contains("SCHOOL_DB_OFFLINE"));
    }

    @Test
    void onlyRealPdfFilesAreAcceptedForUpload() throws Exception {
        MaterialService ms = new MaterialService(mock(com.org.pdfplatform.school.repo.MaterialRepository.class), mock(com.org.pdfplatform.school.repo.MaterialDataRepository.class));
        MockMultipartFile fake = new MockMultipartFile("file", "x.pdf", "application/pdf", "not a pdf".getBytes());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ms.store(fake, 7, "Science", "t", null, "admin")).getMessage().contains("PDF"));
        assertThrows(IllegalArgumentException.class, () -> ms.store(null, 7, "Science", "t", null, "admin"));
        MockMultipartFile real = new MockMultipartFile("file", "x.pdf", "application/pdf", "%PDF-1.7 ...".getBytes());
        assertThrows(IllegalArgumentException.class, () -> ms.store(real, 11, "Science", "t", null, "admin"));
    }
}
