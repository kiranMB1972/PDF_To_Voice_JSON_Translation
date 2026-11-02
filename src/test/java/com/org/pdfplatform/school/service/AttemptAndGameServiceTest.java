package com.org.pdfplatform.school.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.pdfplatform.school.model.GameScore;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.repo.GameScoreRepository;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.repo.TestAttemptRepository;
import com.org.pdfplatform.school.testing.QuestionSpec;
import com.org.pdfplatform.school.testing.TestSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttemptAndGameServiceTest {

    TestAttemptRepository repo;
    AttemptService svc;
    TestDef def;
    TestSpec spec;
    static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 1, 10, 0, 0);

    @BeforeEach
    void setUp() {
        repo = mock(TestAttemptRepository.class);
        when(repo.save(any(TestAttempt.class))).thenAnswer(i -> i.getArgument(0));
        svc = new AttemptService(repo, new ObjectMapper());
        def = new TestDef();
        def.setId(3L);
        def.setDurationMinutes(20);
        spec = new TestSpec("T", 7, "Science", LocalDate.of(2026, 11, 1), 20, null, List.of(5L),
                List.of(new QuestionSpec("Q1", List.of("a", "b"), new LinkedHashSet<>(List.of("B")), 2), new QuestionSpec("Q2", List.of("a", "b"), new LinkedHashSet<>(List.of("A")), 1)));
    }

    private TestAttempt started() {
        TestAttempt a = new TestAttempt();
        a.setTestId(3L);
        a.setStudentId(5L);
        a.setStartedAt(T0);
        return a;
    }

    @Test
    void theTimerCountsDownFromTheMomentTheStudentOpenedTheTest() {
        assertEquals(1200, svc.secondsLeft(def, started(), T0));
        assertEquals(900, svc.secondsLeft(def, started(), T0.plusMinutes(5)));
        assertTrue(svc.secondsLeft(def, started(), T0.plusMinutes(21)) < 0);
    }

    @Test
    void submittingGradesStoresTheAnswersAndClosesTheAttempt() {
        TestAttempt a = svc.submit(def, spec, started(), Map.of(1, Set.of("B"), 2, Set.of("B")), T0.plusMinutes(10));
        assertEquals(2, a.getScore());
        assertEquals(3, a.getTotal());
        assertEquals(T0.plusMinutes(10), a.getSubmittedAt());
        assertEquals(List.of("B"), svc.answers(a).get(1));
    }

    @Test
    void answersThatArriveLongAfterTheTimeIsOverCountAsBlank() {
        TestAttempt a = svc.submit(def, spec, started(), Map.of(1, Set.of("B")), T0.plusMinutes(25));
        assertEquals(0, a.getScore());
    }

    @Test
    void aSubmittedAttemptCannotBeSubmittedAgain() {
        TestAttempt a = svc.submit(def, spec, started(), Map.of(1, Set.of("B")), T0.plusMinutes(5));
        TestAttempt again = svc.submit(def, spec, a, Map.of(1, Set.of("A"), 2, Set.of("A")), T0.plusMinutes(6));
        assertEquals(2, again.getScore());
    }

    @Test
    void gameScoresAreCheckedAndTheBestIsReturned() {
        GameScoreRepository scores = mock(GameScoreRepository.class);
        GameScore best = new GameScore();
        best.setScore(420);
        when(scores.findFirstByGameAndStudentIdOrderByScoreDesc("math-sprint", 5L)).thenReturn(Optional.of(best));
        GameScoreService g = new GameScoreService(scores, mock(SchoolUserRepository.class));
        assertEquals(420, g.save(5L, "math-sprint", 300));
        assertThrows(IllegalArgumentException.class, () -> g.save(5L, "hack-game", 10));
        assertThrows(IllegalArgumentException.class, () -> g.save(5L, "math-sprint", -1));
        assertThrows(IllegalArgumentException.class, () -> g.save(5L, "math-sprint", 999999));
    }
}
