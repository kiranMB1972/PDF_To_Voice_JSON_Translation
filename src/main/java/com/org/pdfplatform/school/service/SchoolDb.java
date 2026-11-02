package com.org.pdfplatform.school.service;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.model.SchoolSetting;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.repo.SchoolSettingRepository;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Makes the School module ready on first use: creates the sch_* tables (CREATE TABLE IF NOT EXISTS - other tables in the
 * same database are never touched), the school record and the first admin. If the cloud database cannot be reached the
 * School screens get the visual "database offline" page and the PDF reader is not affected. Retries every 20 seconds.
 */
@Component
public class SchoolDb {

    private static final Logger log = LoggerFactory.getLogger(SchoolDb.class);
    private static final long RETRY_AFTER_MS = 20_000;

    private final DataSource dataSource;
    private final SchoolUserRepository users;
    private final SchoolSettingRepository settings;
    private final PasswordEncoder encoder;
    private final String adminName;
    private final String adminPassword;
    private volatile boolean ready;
    private volatile long lastFailure;
    private volatile String lastMessage = "";

    public SchoolDb(DataSource dataSource, SchoolUserRepository users, SchoolSettingRepository settings, PasswordEncoder encoder,
                    @Value("${school.admin.username:admin}") String adminName,
                    @Value("${school.admin.initial-password:}") String adminPassword) {
        this.dataSource = dataSource;
        this.users = users;
        this.settings = settings;
        this.encoder = encoder;
        this.adminName = adminName;
        this.adminPassword = adminPassword;
    }

    public synchronized void ensureReady() {
        if (ready) {
            return;
        }
        if (lastFailure != 0 && System.currentTimeMillis() - lastFailure < RETRY_AFTER_MS) {
            throw offline(lastMessage);
        }
        try {
            createTables();
            seed();
            ready = true;
            lastFailure = 0;
        } catch (Exception e) {
            lastFailure = System.currentTimeMillis();
            lastMessage = rootMessage(e);
            log.warn("School database not reachable: {}", lastMessage);
            throw offline(lastMessage);
        }
    }

    private static SchoolException offline(String why) {
        return new SchoolException(ErrorCodeModel.SCHOOL_DB_OFFLINE, 503, "The school database could not be reached: " + why,
                "Check the internet connection and that the Aiven MySQL service is powered on, then retry (the app retries by itself every 20 seconds).");
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        m = m.replaceAll("\\s+", " ");
        return m.length() > 200 ? m.substring(0, 200) : m;
    }

    private void createTables() throws IOException, SQLException {
        String sql = new String(new ClassPathResource("school/schema.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            for (String part : sql.split(";")) {
                String s = part.replaceAll("(?m)^--.*$", "").trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
        }
    }

    private void seed() throws IOException {
        if (!settings.existsById(1L)) {
            SchoolSetting s = new SchoolSetting();
            s.setId(1L);
            s.setSchoolName("My School");
            s.setUpdatedAt(LocalDateTime.now());
            settings.save(s);
        }
        if (!users.existsByRole(Role.ADMIN)) {
            String pw = adminPassword == null || adminPassword.isBlank() ? PasswordGenerator.generate(10) : adminPassword;
            SchoolUser a = new SchoolUser();
            a.setUsername(adminName);
            a.setPasswordHash(encoder.encode(pw));
            a.setRole(Role.ADMIN);
            a.setFullName("Administrator");
            a.setActive(true);
            a.setMustChangePassword(true);
            a.setCreatedAt(LocalDateTime.now());
            users.save(a);
            Path f = Path.of("config", "FIRST_ADMIN_LOGIN.txt");
            Files.createDirectories(f.getParent());
            Files.writeString(f, "First Admin login (change the password after the first login, then delete this file)\nUsername: " + adminName
                    + "\nPassword: " + pw + "\n", StandardCharsets.UTF_8);
            log.warn("First ADMIN created: user '{}', password in {}", adminName, f.toAbsolutePath());
        }
    }
}
