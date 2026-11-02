package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.TestAssignment;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.repo.TestAssignmentRepository;
import com.org.pdfplatform.school.repo.TestAttemptRepository;
import com.org.pdfplatform.school.repo.TestDefRepository;
import com.org.pdfplatform.school.testing.QuestionSpec;
import com.org.pdfplatform.school.testing.TestFileNames;
import com.org.pdfplatform.school.testing.TestJsonExporter;
import com.org.pdfplatform.school.testing.TestPropertiesCodec;
import com.org.pdfplatform.school.testing.TestSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Creates / edits tests. Saving a test generates Test&lt;DD-MM-YYYY&gt;.properties (kept in the DB and in the tests folder). */
@Service
public class TestService {

    private static final Logger log = LoggerFactory.getLogger(TestService.class);

    /** A question as sent by the test builder screen. */
    public static class QuestionDto {
        private String text;
        private List<String> options;
        private List<String> correct;
        private Integer marks;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public List<String> getCorrect() { return correct; }
        public void setCorrect(List<String> correct) { this.correct = correct; }
        public Integer getMarks() { return marks; }
        public void setMarks(Integer marks) { this.marks = marks; }
    }

    private final TestDefRepository defs;
    private final TestAssignmentRepository assignments;
    private final TestAttemptRepository attempts;
    private final Path testsDir;

    public TestService(TestDefRepository defs, TestAssignmentRepository assignments, TestAttemptRepository attempts,
                       @Value("${school.tests-dir:./tests}") String testsDir) {
        this.defs = defs;
        this.assignments = assignments;
        this.attempts = attempts;
        this.testsDir = Path.of(testsDir);
    }

    @Transactional
    public TestDef save(Long id, String title, Integer classLevel, String subject, LocalDate date, Integer duration, Long materialId,
                        List<Long> studentIds, List<QuestionDto> questions, String createdBy) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("The test needs a title.");
        }
        if (classLevel == null || classLevel < 1 || classLevel > 10) {
            throw new IllegalArgumentException("Class must be 1 to 10.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Choose a subject.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Choose the test date.");
        }
        if (duration == null || duration < 1 || duration > 300) {
            throw new IllegalArgumentException("Duration must be 1 to 300 minutes.");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("Add at least one question.");
        }
        List<QuestionSpec> specs = new ArrayList<>();
        int n = 1;
        for (QuestionDto q : questions) {
            specs.add(toSpec(q, n++));
        }
        List<Long> students = studentIds == null ? new ArrayList<>() : studentIds.stream().distinct().collect(Collectors.toList());
        if (students.isEmpty()) {
            throw new IllegalArgumentException("Select at least one student for this test.");
        }

        TestDef def = id == null ? new TestDef() : defs.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown test"));
        String oldFile = def.getPropertiesFile();
        LocalDate oldDate = def.getTestDate();
        boolean newFileName = def.getId() == null || oldDate == null || !oldDate.equals(date);
        String fileName = newFileName ? freeFileName(date, def.getId()) : oldFile;

        TestSpec spec = new TestSpec(title.trim(), classLevel, subject.trim(), date, duration, materialId, students, specs);
        String text = TestPropertiesCodec.write(spec);

        def.setTitle(title.trim());
        def.setClassLevel(classLevel);
        def.setSubject(subject.trim());
        def.setTestDate(date);
        def.setDurationMinutes(duration);
        def.setMaterialId(materialId);
        def.setPropertiesFile(fileName);
        def.setPropertiesText(text);
        if (def.getId() == null) {
            def.setActive(true);
            def.setCreatedBy(createdBy);
            def.setCreatedAt(LocalDateTime.now());
        }
        TestDef saved = defs.save(def);

        assignments.deleteByTestId(saved.getId());
        for (Long sid : students) {
            TestAssignment a = new TestAssignment();
            a.setTestId(saved.getId());
            a.setStudentId(sid);
            assignments.save(a);
        }
        writeFile(fileName, text);
        writeFile(jsonName(fileName), TestJsonExporter.toJson(spec, jsonName(fileName), true));
        if (newFileName && oldFile != null && !oldFile.equals(fileName)) {
            deleteFile(oldFile);
            deleteFile(jsonName(oldFile));
        }
        return saved;
    }

    private QuestionSpec toSpec(QuestionDto q, int number) {
        if (q.getText() == null || q.getText().isBlank()) {
            throw new IllegalArgumentException("Question " + number + " has no text.");
        }
        List<String> opts = new ArrayList<>();
        if (q.getOptions() != null) {
            for (String o : q.getOptions()) {
                if (o == null || o.isBlank()) {
                    throw new IllegalArgumentException("Question " + number + ": fill in every option or remove the empty one.");
                }
                opts.add(o.trim());
            }
        }
        if (opts.size() < 2 || opts.size() > 6) {
            throw new IllegalArgumentException("Question " + number + " needs 2 to 6 options.");
        }
        Set<String> correct = new LinkedHashSet<>();
        if (q.getCorrect() != null) {
            for (String c : q.getCorrect()) {
                String l = c == null ? "" : c.trim().toUpperCase();
                int idx = l.length() == 1 ? l.charAt(0) - 'A' : -1;
                if (idx < 0 || idx >= opts.size()) {
                    throw new IllegalArgumentException("Question " + number + ": a correct answer points to a missing option.");
                }
                correct.add(l);
            }
        }
        if (correct.isEmpty()) {
            throw new IllegalArgumentException("Question " + number + ": tick at least one correct answer.");
        }
        int marks = q.getMarks() == null ? 1 : q.getMarks();
        if (marks < 1 || marks > 100) {
            throw new IllegalArgumentException("Question " + number + ": marks must be 1 to 100.");
        }
        return new QuestionSpec(q.getText().trim(), opts, correct, marks);
    }

    /** Test29-10-2026.properties, or _2, _3 ... when that name is already used by another test. */
    private String freeFileName(LocalDate date, Long ownId) {
        Set<String> used = new HashSet<>();
        for (TestDef d : defs.findByPropertiesFileStartingWith("Test" + TestFileNames.DMY.format(date))) {
            if (ownId == null || !ownId.equals(d.getId())) {
                used.add(d.getPropertiesFile());
            }
        }
        int seq = 1;
        while (used.contains(TestFileNames.forDate(date, seq))) {
            seq++;
        }
        return TestFileNames.forDate(date, seq);
    }

    private void writeFile(String name, String text) {
        try {
            Files.createDirectories(testsDir);
            Files.writeString(testsDir.resolve(name), text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not write {} to {} (the copy stored in the database is still complete): {}", name, testsDir, e.getMessage());
        }
    }

    private void deleteFile(String name) {
        try {
            Files.deleteIfExists(testsDir.resolve(name));
        } catch (IOException e) {
            log.debug("Could not delete old test file {}", name);
        }
    }

    public java.util.Optional<TestDef> find(Long id) {
        return defs.findById(id);
    }

    public static String jsonName(String propertiesFile) {
        return propertiesFile.endsWith(".properties") ? propertiesFile.substring(0, propertiesFile.length() - 11) + ".json" : propertiesFile + ".json";
    }

    public String json(TestDef def, boolean withAnswers) {
        return TestJsonExporter.toJson(spec(def), jsonName(def.getPropertiesFile()), withAnswers);
    }

    public TestSpec spec(TestDef def) {
        return TestPropertiesCodec.read(def.getPropertiesText());
    }

    public List<TestDef> all() {
        return defs.findAllByOrderByTestDateDescIdDesc();
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        TestDef d = defs.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown test"));
        d.setActive(active);
        defs.save(d);
    }

    @Transactional
    public void delete(Long id) {
        TestDef d = defs.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown test"));
        attempts.deleteByTestId(id);
        assignments.deleteByTestId(id);
        defs.delete(d);
        deleteFile(d.getPropertiesFile());
        deleteFile(jsonName(d.getPropertiesFile()));
    }

    /** The student's list: every test assigned to them, with today's tests marked OPEN. */
    public List<TestRow> rowsFor(SchoolUser student, LocalDate today) {
        List<TestRow> rows = new ArrayList<>();
        for (TestAssignment a : assignments.findByStudentId(student.getId())) {
            TestDef d = defs.findById(a.getTestId()).orElse(null);
            if (d == null || !d.isActive()) {
                continue;
            }
            TestAttempt at = attempts.findByTestIdAndStudentId(d.getId(), student.getId()).orElse(null);
            String status;
            if (at != null && at.getSubmittedAt() != null) {
                status = "DONE";
            } else if (d.getTestDate().isAfter(today)) {
                status = "UPCOMING";
            } else if (d.getTestDate().isBefore(today)) {
                status = "MISSED";
            } else {
                status = at != null ? "IN_PROGRESS" : "OPEN";
            }
            rows.add(new TestRow(d, at, status));
        }
        rows.sort((x, y) -> y.getTest().getTestDate().compareTo(x.getTest().getTestDate()));
        return rows;
    }

    public boolean isAssigned(Long testId, Long studentId) {
        return assignments.existsByTestIdAndStudentId(testId, studentId);
    }

    public List<Long> assignedStudentIds(Long testId) {
        return assignments.findByTestId(testId).stream().map(TestAssignment::getStudentId).collect(Collectors.toList());
    }

    public Map<Long, TestAttempt> attemptsByStudent(Long testId) {
        return attempts.findByTestId(testId).stream().collect(Collectors.toMap(TestAttempt::getStudentId, a -> a, (x, y) -> x));
    }
}
