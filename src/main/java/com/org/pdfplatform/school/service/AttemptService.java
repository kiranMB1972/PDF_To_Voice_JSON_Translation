package com.org.pdfplatform.school.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.repo.TestAttemptRepository;
import com.org.pdfplatform.school.testing.Grader;
import com.org.pdfplatform.school.testing.TestSpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AttemptService {

    private static final long GRACE_SECONDS = 90;      // network delay allowance when the timer auto-submits

    private final TestAttemptRepository attempts;
    private final ObjectMapper mapper;

    public AttemptService(TestAttemptRepository attempts, ObjectMapper mapper) {
        this.attempts = attempts;
        this.mapper = mapper;
    }

    @Transactional
    public TestAttempt startOrResume(TestDef def, Long studentId, LocalDateTime now) {
        return attempts.findByTestIdAndStudentId(def.getId(), studentId).orElseGet(() -> {
            TestAttempt a = new TestAttempt();
            a.setTestId(def.getId());
            a.setStudentId(studentId);
            a.setStartedAt(now);
            return attempts.save(a);
        });
    }

    public long secondsLeft(TestDef def, TestAttempt a, LocalDateTime now) {
        long total = def.getDurationMinutes() * 60L;
        return total - Duration.between(a.getStartedAt(), now).getSeconds();
    }

    /** Grades and closes the attempt. Answers that arrive long after the time is over count as blank. */
    @Transactional
    public TestAttempt submit(TestDef def, TestSpec spec, TestAttempt a, Map<Integer, Set<String>> answers, LocalDateTime now) {
        if (a.getSubmittedAt() != null) {
            return a;
        }
        boolean late = secondsLeft(def, a, now) < -GRACE_SECONDS;
        Map<Integer, Set<String>> used = late ? new HashMap<>() : answers;
        Grader.Result r = Grader.grade(spec, used);
        Map<Integer, List<String>> plain = new LinkedHashMap<>();
        for (Map.Entry<Integer, Set<String>> e : used.entrySet()) {
            plain.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        try {
            a.setAnswersJson(mapper.writeValueAsString(plain));
        } catch (JsonProcessingException e) {
            a.setAnswersJson("{}");
        }
        a.setScore(r.score());
        a.setTotal(r.total());
        a.setSubmittedAt(now);
        return attempts.save(a);
    }

    public Map<Integer, List<String>> answers(TestAttempt a) {
        if (a.getAnswersJson() == null || a.getAnswersJson().isBlank()) {
            return new HashMap<>();
        }
        try {
            return mapper.readValue(a.getAnswersJson(), new TypeReference<Map<Integer, List<String>>>() { });
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    public List<TestAttempt> forStudent(Long studentId) {
        return attempts.findByStudentIdOrderByStartedAtDesc(studentId);
    }

    public TestAttempt find(Long testId, Long studentId) {
        return attempts.findByTestIdAndStudentId(testId, studentId).orElse(null);
    }
}
