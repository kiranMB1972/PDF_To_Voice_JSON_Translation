package com.org.pdfplatform.school.web;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.SchoolException;
import com.org.pdfplatform.school.model.LiveSession;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.Role;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.TestAttempt;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.repo.SchoolUserRepository;
import com.org.pdfplatform.school.service.LiveService;
import com.org.pdfplatform.school.service.MaterialService;
import com.org.pdfplatform.school.service.SchoolService;
import com.org.pdfplatform.school.service.TestService;
import com.org.pdfplatform.school.service.UserService;
import com.org.pdfplatform.school.testing.QuestionSpec;
import com.org.pdfplatform.school.testing.TestSpec;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Everything the school admin does: school name, students, PDFs, tests, results and the live class. */
@RestController
@RequestMapping("/school/api/admin")
public class SchoolAdminApi {

    private final SchoolService school;
    private final UserService userService;
    private final SchoolUserRepository users;
    private final MaterialService materials;
    private final TestService tests;
    private final LiveService live;
    private final ZoneId zone;

    public SchoolAdminApi(SchoolService school, UserService userService, SchoolUserRepository users, MaterialService materials, TestService tests,
                          LiveService live, @Value("${school.zone:Asia/Kolkata}") String zone) {
        this.school = school;
        this.userService = userService;
        this.users = users;
        this.materials = materials;
        this.tests = tests;
        this.live = live;
        this.zone = ZoneId.of(zone);
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        LocalDate today = LocalDate.now(zone);
        long testsToday = tests.all().stream().filter(t -> t.getTestDate().equals(today)).count();
        return Views.m("schoolName", school.name(), "students", users.countByRole(Role.STUDENT), "materials", materials.all().size(),
                "testsToday", testsToday, "liveClasses", live.allActive().size(), "today", today.toString());
    }

    /** Only /school/api/admin/** (ADMIN role) reaches this, so only the admin can rename the school. */
    @PutMapping("/school")
    public Map<String, Object> rename(@RequestBody Map<String, String> body) {
        school.rename(body.get("schoolName"));
        return Views.m("schoolName", school.name());
    }

    // ------------------------------------------------------------------------------------------------ students
    @GetMapping("/students")
    public List<Map<String, Object>> students(@RequestParam(required = false) Integer classLevel) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SchoolUser u : users.findByRoleOrderByClassLevelAscRollNoAscFullNameAsc(Role.STUDENT)) {
            if (classLevel == null || classLevel.equals(u.getClassLevel())) {
                out.add(Views.user(u));
            }
        }
        return out;
    }

    private static Map<String, Object> created(List<UserService.Created> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserService.Created c : list) {
            rows.add(Views.m("username", c.getUsername(), "fullName", c.getFullName(), "classLevel", c.getClassLevel(), "password", c.getPassword()));
        }
        return Views.m("created", rows);
    }

    @PostMapping("/students")
    public Map<String, Object> addStudent(@RequestBody Map<String, Object> b) {
        Integer cl = b.get("classLevel") == null ? null : Integer.valueOf(String.valueOf(b.get("classLevel")));
        UserService.Created c = userService.addStudent(String.valueOf(b.getOrDefault("username", "")), String.valueOf(b.getOrDefault("fullName", "")), cl,
                b.get("rollNo") == null ? null : String.valueOf(b.get("rollNo")), b.get("password") == null ? null : String.valueOf(b.get("password")));
        return created(List.of(c));
    }

    @PostMapping("/students/bulk")
    public Map<String, Object> bulk(@RequestBody Map<String, String> b) {
        List<String> problems = new ArrayList<>();
        Map<String, Object> out = created(userService.bulkAdd(b.get("lines"), problems));
        out.put("problems", problems);
        return out;
    }

    @PostMapping("/students/{id}/reset")
    public Map<String, Object> reset(@PathVariable Long id) {
        String pw = userService.resetPassword(id);
        String name = users.findById(id).map(SchoolUser::getUsername).orElse("?");
        return created(List.of(new UserService.Created(name, "", null, pw)));
    }

    @PostMapping("/students/{id}/active")
    public Map<String, Object> active(@PathVariable Long id, @RequestBody Map<String, Boolean> b) {
        userService.setActive(id, Boolean.TRUE.equals(b.get("active")));
        return Views.m("ok", true);
    }

    @DeleteMapping("/students/{id}")
    public Map<String, Object> deleteStudent(@PathVariable Long id) {
        userService.deleteStudent(id);
        return Views.m("ok", true);
    }

    // ------------------------------------------------------------------------------------------------ PDFs
    @GetMapping("/materials")
    public List<Map<String, Object>> materials() {
        Map<Long, String> names = new HashMap<>();
        for (SchoolUser u : users.findByRoleOrderByClassLevelAscRollNoAscFullNameAsc(Role.STUDENT)) {
            names.put(u.getId(), u.getFullName());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Material m : materials.all()) {
            out.add(Views.material(m, m.getTargetStudentId() == null ? null : names.get(m.getTargetStudentId())));
        }
        return out;
    }

    @PostMapping("/materials")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file, @RequestParam Integer classLevel, @RequestParam String subject,
                                      @RequestParam(required = false) String title, @RequestParam(required = false) Long targetStudentId,
                                      HttpServletRequest req) throws IOException {
        if (targetStudentId != null) {
            SchoolUser st = users.findById(targetStudentId).orElseThrow(() -> new IllegalArgumentException("Unknown student"));
            if (st.getClassLevel() == null || !st.getClassLevel().equals(classLevel)) {
                throw new IllegalArgumentException("That student is not in class " + classLevel + ".");
            }
        }
        Material m = materials.store(file, classLevel, subject, title, targetStudentId, SchoolSession.user(req).getUsername());
        return Views.m("id", m.getId());
    }

    @DeleteMapping("/materials/{id}")
    public Map<String, Object> deleteMaterial(@PathVariable Long id) {
        materials.delete(id);
        return Views.m("ok", true);
    }

    // ------------------------------------------------------------------------------------------------ tests
    @GetMapping("/tests")
    public List<Map<String, Object>> tests() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (TestDef d : tests.all()) {
            out.add(Views.test(d));
        }
        return out;
    }

    @GetMapping("/tests/{id}")
    public Map<String, Object> test(@PathVariable Long id) {
        TestDef d = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        TestSpec spec = tests.spec(d);
        List<Map<String, Object>> qs = new ArrayList<>();
        for (QuestionSpec q : spec.questions()) {
            qs.add(Views.m("text", q.text(), "options", q.options(), "correct", new TreeSet<>(q.correct()), "marks", q.marks()));
        }
        Map<String, Object> out = Views.test(d);
        out.put("studentIds", tests.assignedStudentIds(id));
        out.put("questions", qs);
        return out;
    }

    /** Saves the test and generates Test&lt;DD-MM-YYYY&gt;.properties (and .json). */
    @PostMapping("/tests")
    public Map<String, Object> saveTest(@RequestBody SaveTestRequest r, HttpServletRequest req) {
        LocalDate date = r.getTestDate() == null || r.getTestDate().isBlank() ? LocalDate.now(zone) : LocalDate.parse(r.getTestDate());
        TestDef d = tests.save(r.getId(), r.getTitle(), r.getClassLevel(), r.getSubject(), date, r.getDurationMinutes(), r.getMaterialId(),
                r.getStudentIds(), r.getQuestions(), SchoolSession.user(req).getUsername());
        return Views.m("id", d.getId(), "propertiesFile", d.getPropertiesFile(), "jsonFile", TestService.jsonName(d.getPropertiesFile()), "date", d.getTestDate().toString());
    }

    @DeleteMapping("/tests/{id}")
    public Map<String, Object> deleteTest(@PathVariable Long id) {
        tests.delete(id);
        return Views.m("ok", true);
    }

    @PostMapping("/tests/{id}/active")
    public Map<String, Object> testActive(@PathVariable Long id, @RequestBody Map<String, Boolean> b) {
        tests.setActive(id, Boolean.TRUE.equals(b.get("active")));
        return Views.m("ok", true);
    }

    @GetMapping("/tests/{id}/properties")
    public ResponseEntity<byte[]> properties(@PathVariable Long id) {
        TestDef d = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        return download(d.getPropertiesFile(), "text/plain; charset=UTF-8", d.getPropertiesText());
    }

    @GetMapping("/tests/{id}/json")
    public ResponseEntity<byte[]> json(@PathVariable Long id) {
        TestDef d = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        return download(TestService.jsonName(d.getPropertiesFile()), "application/json; charset=UTF-8", tests.json(d, true));
    }

    @GetMapping("/tests/{id}/results")
    public List<Map<String, Object>> results(@PathVariable Long id) {
        Map<Long, TestAttempt> attempts = tests.attemptsByStudent(id);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Long sid : tests.assignedStudentIds(id)) {
            SchoolUser u = users.findById(sid).orElse(null);
            if (u == null) {
                continue;
            }
            TestAttempt a = attempts.get(sid);
            String status = a == null ? "NOT_STARTED" : a.getSubmittedAt() == null ? "IN_PROGRESS" : "DONE";
            Integer pct = a != null && a.getSubmittedAt() != null && a.getTotal() != null && a.getTotal() > 0 ? Math.round(100f * a.getScore() / a.getTotal()) : null;
            out.add(Views.m("studentId", sid, "name", u.getFullName(), "classLevel", u.getClassLevel(), "rollNo", u.getRollNo(), "status", status,
                    "score", a == null ? null : a.getScore(), "total", a == null ? null : a.getTotal(), "percent", pct,
                    "submittedAt", a == null || a.getSubmittedAt() == null ? null : a.getSubmittedAt().toString()));
        }
        return out;
    }

    @GetMapping("/tests/{id}/results.csv")
    public ResponseEntity<byte[]> resultsCsv(@PathVariable Long id) {
        TestDef d = tests.find(id).orElseThrow(() -> new SchoolException(ErrorCodeModel.NOT_FOUND, 404, "That test does not exist", null));
        StringBuilder sb = new StringBuilder("\uFEFFName,Class,Roll no,Status,Score,Total,Percent\n");
        for (Map<String, Object> r : results(id)) {
            sb.append(csv(r.get("name"))).append(',').append(csv(r.get("classLevel"))).append(',').append(csv(r.get("rollNo"))).append(',')
                    .append(csv(r.get("status"))).append(',').append(csv(r.get("score"))).append(',').append(csv(r.get("total"))).append(',').append(csv(r.get("percent"))).append('\n');
        }
        return download("Results_" + d.getPropertiesFile().replace(".properties", "") + ".csv", "text/csv; charset=UTF-8", sb.toString());
    }

    private static String csv(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static ResponseEntity<byte[]> download(String name, String type, String text) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType(type)).body(text.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------------------------------------ live class
    @GetMapping("/live")
    public List<Map<String, Object>> liveList() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (LiveSession s : live.allActive()) {
            out.add(Views.live(s, materials.find(s.getMaterialId()).map(Material::getTitle).orElse("?"), live.recent(s.getId())));
        }
        return out;
    }

    @PostMapping("/live/start")
    public Map<String, Object> liveStart(@RequestBody Map<String, Object> b) {
        LiveSession s = live.start(Integer.valueOf(String.valueOf(b.get("classLevel"))), b.get("materialId") == null ? null : Long.valueOf(String.valueOf(b.get("materialId"))),
                b.get("title") == null ? null : String.valueOf(b.get("title")));
        return Views.m("sessionId", s.getId());
    }

    @PostMapping("/live/{id}/stop")
    public Map<String, Object> liveStop(@PathVariable Long id) {
        live.stop(id);
        return Views.m("ok", true);
    }

    @PostMapping("/live/{id}/page")
    public Map<String, Object> livePage(@PathVariable Long id, @RequestBody Map<String, Integer> b) {
        live.setPage(id, b.getOrDefault("page", 1));
        return Views.m("ok", true);
    }

    @PostMapping("/live/{id}/message")
    public Map<String, Object> liveMessage(@PathVariable Long id, @RequestBody Map<String, String> b, HttpServletRequest req) {
        live.post(id, SchoolSession.user(req), b.get("text"));
        return Views.m("ok", true);
    }

    /** Body of POST /tests (the test builder). */
    public static class SaveTestRequest {
        private Long id;
        private String title;
        private Integer classLevel;
        private String subject;
        private String testDate;
        private Integer durationMinutes;
        private Long materialId;
        private List<Long> studentIds;
        private List<TestService.QuestionDto> questions;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Integer getClassLevel() { return classLevel; }
        public void setClassLevel(Integer classLevel) { this.classLevel = classLevel; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getTestDate() { return testDate; }
        public void setTestDate(String testDate) { this.testDate = testDate; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
        public Long getMaterialId() { return materialId; }
        public void setMaterialId(Long materialId) { this.materialId = materialId; }
        public List<Long> getStudentIds() { return studentIds; }
        public void setStudentIds(List<Long> studentIds) { this.studentIds = studentIds; }
        public List<TestService.QuestionDto> getQuestions() { return questions; }
        public void setQuestions(List<TestService.QuestionDto> questions) { this.questions = questions; }
    }
}
