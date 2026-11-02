package com.org.pdfplatform.school.web;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.LiveSession;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.service.AttemptService;
import com.org.pdfplatform.school.service.GameScoreService;
import com.org.pdfplatform.school.service.LiveService;
import com.org.pdfplatform.school.service.MaterialService;
import com.org.pdfplatform.school.service.TestRow;
import com.org.pdfplatform.school.service.TestService;
import com.org.pdfplatform.school.testing.QuestionSpec;
import com.org.pdfplatform.school.testing.TestFileNames;
import com.org.pdfplatform.school.testing.TestSpec;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** What a student does: today's tests, PDFs, the live class, results and the education games. */
@RestController
@RequestMapping("/school/api/student")
public class SchoolStudentApi {

    private final TestService tests;
    private final AttemptService attempts;
    private final MaterialService materials;
    private final LiveService live;
    private final GameScoreService games;
    private final ZoneId zone;
    private final boolean showAnswers;

    public SchoolStudentApi(TestService tests, AttemptService attempts, MaterialService materials, LiveService live, GameScoreService games,
                            @Value("${school.zone:Asia/Kolkata}") String zone, @Value("${school.show-answers-after-submit:true}") boolean showAnswers) {
        this.tests = tests;
        this.attempts = attempts;
        this.materials = materials;
        this.live = live;
        this.games = games;
        this.zone = ZoneId.of(zone);
        this.showAnswers = showAnswers;
    }

    @GetMapping("/home")
    public Map<String, Object> home(HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        LocalDate today = LocalDate.now(zone);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TestRow r : tests.rowsFor(me, today)) {
            rows.add(Views.row(r));
        }
        return Views.m("name", me.getFullName(), "classLevel", me.getClassLevel(), "today", today.toString(), "tests", rows,
                "liveActive", live.activeFor(me.getClassLevel()).isPresent(), "materials", materials.visibleTo(me).size());
    }

    @GetMapping("/materials")
    public List<Map<String, Object>> materials(HttpServletRequest req) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Material m : materials.visibleTo(SchoolSession.user(req))) {
            out.add(Views.material(m, null));
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ tests
    private static SchoolException closed(String msg, String guidance) {
        return new SchoolException(ErrorCodeModel.TEST_NOT_OPEN, 409, msg, guidance);
    }

    /** Opens (or resumes) today's test. The correct answers never leave the server. */
    @GetMapping("/tests/{id}/start")
    public Map<String, Object> start(@PathVariable Long id, HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        TestDef def = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        if (!def.isActive() || !tests.isAssigned(id, me.getId())) {
            throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "This test is not assigned to you", null);
        }
        LocalDate today = LocalDate.now(zone);
        if (!def.getTestDate().equals(today)) {
            throw closed("This test opens on " + TestFileNames.DMY.format(def.getTestDate()), "Come back on that date - it opens by itself.");
        }
        LocalDateTime now = LocalDateTime.now(zone);
        TestAttempt a = attempts.startOrResume(def, me.getId(), now);
        TestSpec spec = tests.spec(def);
        if (a.getSubmittedAt() != null) {
            throw closed("You have already submitted this test", "Open Results to see your marks.");
        }
        long left = attempts.secondsLeft(def, a, now);
        if (left <= 0) {
            attempts.submit(def, spec, a, new HashMap<>(), now);
            throw new SchoolException(ErrorCodeModel.TEST_TIME_UP, 409, "The time for this test is over", "Open Results to see your marks.");
        }
        List<Map<String, Object>> qs = new ArrayList<>();
        int n = 1;
        for (QuestionSpec q : spec.questions()) {
            List<Map<String, Object>> opts = new ArrayList<>();
            for (int i = 0; i < q.options().size(); i++) {
                opts.add(Views.m("letter", QuestionSpec.letter(i), "text", q.options().get(i)));
            }
            qs.add(Views.m("number", n++, "text", q.text(), "marks", q.marks(), "multiple", q.correct().size() > 1, "options", opts));
        }
        return Views.m("testId", def.getId(), "title", def.getTitle(), "subject", def.getSubject(), "durationMinutes", def.getDurationMinutes(),
                "secondsLeft", left, "totalMarks", spec.totalMarks(), "questions", qs, "attemptKey", a.getId());
    }

    @PostMapping("/tests/{id}/submit")
    public Map<String, Object> submit(@PathVariable Long id, @RequestBody Map<String, Map<String, List<String>>> body, HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        TestDef def = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        TestAttempt a = attempts.find(id, me.getId());
        if (a == null || !tests.isAssigned(id, me.getId())) {
            throw new SchoolException(ErrorCodeModel.ACCESS_DENIED, 403, "You have not started this test", null);
        }
        TestSpec spec = tests.spec(def);
        Map<Integer, Set<String>> answers = new HashMap<>();
        Map<String, List<String>> raw = body.getOrDefault("answers", new HashMap<>());
        for (Map.Entry<String, List<String>> e : raw.entrySet()) {
            int n;
            try {
                n = Integer.parseInt(e.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (n < 1 || n > spec.questions().size()) {
                continue;
            }
            int max = spec.questions().get(n - 1).options().size();
            Set<String> set = new LinkedHashSet<>();
            for (String l : e.getValue() == null ? new ArrayList<String>() : e.getValue()) {
                if (l != null && l.length() == 1 && l.charAt(0) >= 'A' && l.charAt(0) < 'A' + max) {
                    set.add(l);
                }
            }
            answers.put(n, set);
        }
        TestAttempt done = attempts.submit(def, spec, a, answers, LocalDateTime.now(zone));
        return Views.m("score", done.getScore(), "total", done.getTotal(), "percent", done.getTotal() == 0 ? 0 : Math.round(100f * done.getScore() / done.getTotal()));
    }

    @GetMapping("/tests/{id}/result")
    public Map<String, Object> result(@PathVariable Long id, HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        TestDef def = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        TestAttempt a = attempts.find(id, me.getId());
        if (a == null || a.getSubmittedAt() == null) {
            throw closed("You have not submitted this test yet", "Open it from Today's tests.");
        }
        TestSpec spec = tests.spec(def);
        Map<Integer, List<String>> given = attempts.answers(a);
        List<Map<String, Object>> qs = new ArrayList<>();
        int n = 1;
        for (QuestionSpec q : spec.questions()) {
            List<String> mine = given.getOrDefault(n, new ArrayList<>());
            List<Map<String, Object>> opts = new ArrayList<>();
            for (int i = 0; i < q.options().size(); i++) {
                String l = QuestionSpec.letter(i);
                opts.add(Views.m("letter", l, "text", q.options().get(i), "selected", mine.contains(l), "correct", showAnswers ? q.correct().contains(l) : null));
            }
            boolean ok = new LinkedHashSet<>(mine).equals(q.correct());
            qs.add(Views.m("number", n, "text", q.text(), "marks", q.marks(), "earned", ok, "options", opts));
            n++;
        }
        return Views.m("title", def.getTitle(), "subject", def.getSubject(), "score", a.getScore(), "total", a.getTotal(), "questions", qs, "answersShown", showAnswers);
    }

    @GetMapping("/results")
    public List<Map<String, Object>> results(HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        List<Map<String, Object>> out = new ArrayList<>();
        for (TestAttempt a : attempts.forStudent(me.getId())) {
            Optional<TestDef> d = tests.find(a.getTestId());
            if (d.isPresent() && a.getSubmittedAt() != null) {
                out.add(Views.m("testId", a.getTestId(), "title", d.get().getTitle(), "subject", d.get().getSubject(), "date", d.get().getTestDate().toString(),
                        "score", a.getScore(), "total", a.getTotal()));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------ live class
    @GetMapping("/live/state")
    public Map<String, Object> liveState(HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        Optional<LiveSession> s = live.activeFor(me.getClassLevel());
        if (s.isEmpty()) {
            return Views.m("active", false);
        }
        return Views.live(s.get(), materials.find(s.get().getMaterialId()).map(Material::getTitle).orElse("?"), live.recent(s.get().getId()));
    }

    @PostMapping("/live/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> b, HttpServletRequest req) {
        SchoolUser me = SchoolSession.user(req);
        LiveSession s = live.activeFor(me.getClassLevel()).orElseThrow(() -> new SchoolException(ErrorCodeModel.VALIDATION, 409, "There is no live class right now", null));
        live.post(s.getId(), me, b.get("text"));
        return Views.m("ok", true);
    }

    // ------------------------------------------------------------------------------------------------ games
    @PostMapping("/games/score")
    public Map<String, Object> score(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        int best = games.save(SchoolSession.user(req).getId(), String.valueOf(b.get("game")), Integer.parseInt(String.valueOf(b.get("score"))));
        return Views.m("best", best);
    }

    @GetMapping("/games/leaderboard")
    public List<Map<String, Object>> leaderboard(@RequestParam String game) {
        return games.top(game);
    }
}
