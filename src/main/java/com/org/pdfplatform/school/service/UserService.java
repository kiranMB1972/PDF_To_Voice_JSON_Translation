package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.repo.MaterialRepository;
import com.org.pdfplatform.school.repo.TestAssignmentRepository;
import com.org.pdfplatform.school.repo.TestAttemptRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,40}");

    /** A freshly created login, shown ONCE to the admin (the password is stored only as a hash). */
    public static class Created {
        private final String username;
        private final String fullName;
        private final Integer classLevel;
        private final String password;

        public Created(String username, String fullName, Integer classLevel, String password) {
            this.username = username;
            this.fullName = fullName;
            this.classLevel = classLevel;
            this.password = password;
        }

        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public Integer getClassLevel() { return classLevel; }
        public String getPassword() { return password; }
    }

    private final SchoolUserRepository users;
    private final TestAssignmentRepository assignments;
    private final TestAttemptRepository attempts;
    private final MaterialRepository materials;
    private final MaterialService materialService;
    private final PasswordEncoder encoder;

    public UserService(SchoolUserRepository users, TestAssignmentRepository assignments, TestAttemptRepository attempts,
                       MaterialRepository materials, MaterialService materialService, PasswordEncoder encoder) {
        this.users = users;
        this.assignments = assignments;
        this.attempts = attempts;
        this.materials = materials;
        this.materialService = materialService;
        this.encoder = encoder;
    }

    @Transactional
    public Created addStudent(String username, String fullName, Integer classLevel, String rollNo, String rawPassword) {
        String un = username == null ? "" : username.trim();
        if (!USERNAME.matcher(un).matches()) {
            throw new IllegalArgumentException("Username '" + un + "' must be 3-40 letters, digits, dot, dash or underscore.");
        }
        if (classLevel == null || classLevel < 1 || classLevel > 10) {
            throw new IllegalArgumentException("Class must be 1 to 10.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("The student's name is required.");
        }
        if (users.existsByUsernameIgnoreCase(un)) {
            throw new IllegalArgumentException("Username '" + un + "' is already used.");
        }
        String pw = rawPassword == null || rawPassword.isBlank() ? PasswordGenerator.generate(8) : rawPassword;
        if (pw.length() < 6) {
            throw new IllegalArgumentException("The password must have at least 6 characters.");
        }
        SchoolUser u = new SchoolUser();
        u.setUsername(un);
        u.setPasswordHash(encoder.encode(pw));
        u.setRole(Role.STUDENT);
        u.setFullName(fullName.trim());
        u.setClassLevel(classLevel);
        u.setRollNo(rollNo == null ? null : rollNo.trim());
        u.setActive(true);
        u.setMustChangePassword(true);
        u.setCreatedAt(LocalDateTime.now());
        users.save(u);
        return new Created(un, u.getFullName(), classLevel, pw);
    }

    /** One student per line: username,Full Name,class[,roll no]. Bad lines are reported, good lines are created. */
    @Transactional
    public List<Created> bulkAdd(String text, List<String> problems) {
        List<Created> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        int n = 0;
        for (String line : text.split("\\r?\\n")) {
            n++;
            if (line.isBlank()) {
                continue;
            }
            String[] p = line.split(",");
            try {
                if (p.length < 3) {
                    throw new IllegalArgumentException("need username,name,class");
                }
                out.add(addStudent(p[0], p[1], Integer.valueOf(p[2].trim()), p.length > 3 ? p[3] : null, null));
            } catch (NumberFormatException e) {
                problems.add("Line " + n + ": class must be a number 1-10");
            } catch (IllegalArgumentException e) {
                problems.add("Line " + n + ": " + e.getMessage());
            }
        }
        return out;
    }

    @Transactional
    public String resetPassword(Long id) {
        SchoolUser u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
        String pw = PasswordGenerator.generate(8);
        u.setPasswordHash(encoder.encode(pw));
        u.setMustChangePassword(true);
        users.save(u);
        return pw;
    }

    @Transactional
    public void changePassword(SchoolUser u, String oldPassword, String newPassword) {
        if (!encoder.matches(oldPassword == null ? "" : oldPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("The current password is not correct.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("The new password must have at least 6 characters.");
        }
        u.setPasswordHash(encoder.encode(newPassword));
        u.setMustChangePassword(false);
        users.save(u);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        SchoolUser u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
        if (u.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("The admin account cannot be deactivated here.");
        }
        u.setActive(active);
        users.save(u);
    }

    /** Removes the student with everything that belongs only to them (test results, assignments, personal PDFs). */
    @Transactional
    public void deleteStudent(Long id) {
        SchoolUser u = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
        if (u.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("Only student accounts can be deleted.");
        }
        attempts.deleteByStudentId(id);
        assignments.deleteByStudentId(id);
        for (Material m : materials.findByTargetStudentId(id)) {
            materialService.delete(m.getId());
        }
        users.delete(u);
    }
}
