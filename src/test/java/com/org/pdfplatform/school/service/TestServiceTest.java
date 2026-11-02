package com.org.pdfplatform.school.service;

import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.TestAssignment;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.repo.TestAssignmentRepository;
import com.org.pdfplatform.school.repo.TestAttemptRepository;
import com.org.pdfplatform.school.repo.TestDefRepository;
import com.org.pdfplatform.school.testing.TestSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestServiceTest {

    @TempDir
    Path dir;

    TestDefRepository defs;
    TestAssignmentRepository assignments;
    TestAttemptRepository attempts;
    TestService svc;
    static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    @BeforeEach
    void setUp() {
        defs = mock(TestDefRepository.class);
        assignments = mock(TestAssignmentRepository.class);
        attempts = mock(TestAttemptRepository.class);
        when(defs.save(any(TestDef.class))).thenAnswer(i -> {
            TestDef d = i.getArgument(0);
            if (d.getId() == null) {
                d.setId(7L);
            }
            return d;
        });
        when(defs.findByPropertiesFileStartingWith(anyString())).thenReturn(new ArrayList<>());
        svc = new TestService(defs, assignments, attempts, dir.toString());
    }

    private static TestService.QuestionDto q(String text, List<String> options, List<String> correct, Integer marks) {
        TestService.QuestionDto d = new TestService.QuestionDto();
        d.setText(text);
        d.setOptions(options);
        d.setCorrect(correct);
        d.setMarks(marks);
        return d;
    }

    private TestDef save(List<TestService.QuestionDto> qs, List<Long> students) {
        return svc.save(null, "Unit test", 7, "Science", DAY, 20, null, students, qs, "admin");
    }

    private static List<TestService.QuestionDto> oneQuestion() {
        return List.of(q("Which gas do plants take in?", List.of("Oxygen", "Carbon dioxide", "Helium"), List.of("B"), 2));
    }

    @Test
    void savingATestWritesTestDdMmYyyyPropertiesAndItsJsonTwin() throws Exception {
        TestDef d = save(oneQuestion(), List.of(5L, 6L));
        assertEquals("Test01-11-2026.properties", d.getPropertiesFile());
        assertTrue(Files.exists(dir.resolve("Test01-11-2026.properties")));
        assertTrue(Files.exists(dir.resolve("Test01-11-2026.json")));
        String props = Files.readString(dir.resolve("Test01-11-2026.properties"));
        assertTrue(props.contains("question.1.correct=B"));
        assertTrue(props.contains("test.students=5,6"));
        assertEquals(props, d.getPropertiesText());
        TestSpec spec = svc.spec(d);
        assertEquals(2, spec.totalMarks());
    }

    @Test
    void aSecondTestOnTheSameDayGetsASuffixInsteadOfOverwritingTheFirst() {
        TestDef first = new TestDef();
        first.setId(3L);
        first.setPropertiesFile("Test01-11-2026.properties");
        when(defs.findByPropertiesFileStartingWith(anyString())).thenReturn(List.of(first));
        assertEquals("Test01-11-2026_2.properties", save(oneQuestion(), List.of(5L)).getPropertiesFile());
    }

    @Test
    void badInputIsRefusedWithAMessageThatNamesTheProblem() {
        assertThrows(IllegalArgumentException.class, () -> save(List.of(), List.of(5L)));
        assertThrows(IllegalArgumentException.class, () -> save(oneQuestion(), List.of()));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> save(List.of(q("Q?", List.of("a", "b"), List.of(), 1)), List.of(5L))).getMessage().contains("correct"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> save(List.of(q("Q?", List.of("a", ""), List.of("A"), 1)), List.of(5L))).getMessage().contains("option"));
        assertThrows(IllegalArgumentException.class, () -> save(List.of(q("Q?", List.of("a", "b"), List.of("C"), 1)), List.of(5L)));
        assertThrows(IllegalArgumentException.class, () -> svc.save(null, "t", 11, "Maths", DAY, 20, null, List.of(5L), oneQuestion(), "a"));
        assertThrows(IllegalArgumentException.class, () -> svc.save(null, " ", 7, "Maths", DAY, 20, null, List.of(5L), oneQuestion(), "a"));
    }

    @Test
    void theStudentListMarksTodaysTestOpenAndOthersByDate() {
        SchoolUser me = new SchoolUser();
        me.setId(5L);
        String[] expected = {"OPEN", "UPCOMING", "MISSED", "DONE"};
        List<TestAssignment> as = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            TestAssignment a = new TestAssignment();
            a.setTestId(10L + i);
            a.setStudentId(5L);
            as.add(a);
            TestDef d = new TestDef();
            d.setId(10L + i);
            d.setActive(true);
            d.setTestDate(i == 1 ? DAY.plusDays(2) : i == 2 ? DAY.minusDays(1) : DAY);
            when(defs.findById(10L + i)).thenReturn(Optional.of(d));
            if (i == 3) {
                TestAttempt at = new TestAttempt();
                at.setSubmittedAt(LocalDateTime.now());
                when(attempts.findByTestIdAndStudentId(13L, 5L)).thenReturn(Optional.of(at));
            } else {
                when(attempts.findByTestIdAndStudentId(10L + i, 5L)).thenReturn(Optional.empty());
            }
        }
        when(assignments.findByStudentId(5L)).thenReturn(as);
        List<String> statuses = new ArrayList<>();
        svc.rowsFor(me, DAY).forEach(r -> statuses.add(r.getStatus()));
        assertTrue(statuses.containsAll(List.of(expected)) && statuses.size() == 4, statuses.toString());
    }
}
