package com.org.pdfplatform.school.web;

import com.org.pdfplatform.school.model.LiveMessage;
import com.org.pdfplatform.school.model.LiveSession;
import com.org.pdfplatform.school.model.Material;
import com.org.pdfplatform.school.model.SchoolUser;
import com.org.pdfplatform.school.model.TestDef;
import com.org.pdfplatform.school.service.TestRow;
import com.org.pdfplatform.school.testing.TestFileNames;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the JSON the screens need. Entities are never returned directly (they hold password hashes and correct answers). */
final class Views {

    private Views() { }

    /** Map that allows null values: m("a", 1, "b", null). */
    static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            out.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return out;
    }

    static Map<String, Object> user(SchoolUser u) {
        return m("id", u.getId(), "username", u.getUsername(), "fullName", u.getFullName(), "classLevel", u.getClassLevel(),
                "rollNo", u.getRollNo(), "active", u.isActive(), "role", u.getRole().name());
    }

    static Map<String, Object> material(Material x, String targetName) {
        return m("id", x.getId(), "classLevel", x.getClassLevel(), "subject", x.getSubject(), "title", x.getTitle(), "fileName", x.getFileName(),
                "sizeBytes", x.getSizeBytes(), "targetStudentId", x.getTargetStudentId(), "targetName", targetName,
                "uploadedAt", x.getUploadedAt() == null ? null : x.getUploadedAt().toString());
    }

    static Map<String, Object> test(TestDef d) {
        return m("id", d.getId(), "title", d.getTitle(), "subject", d.getSubject(), "classLevel", d.getClassLevel(), "date", d.getTestDate().toString(),
                "dateDMY", TestFileNames.DMY.format(d.getTestDate()), "durationMinutes", d.getDurationMinutes(), "active", d.isActive(),
                "propertiesFile", d.getPropertiesFile(), "materialId", d.getMaterialId());
    }

    static Map<String, Object> row(TestRow r) {
        Map<String, Object> t = test(r.getTest());
        t.put("status", r.getStatus());
        if (r.getAttempt() != null && r.getAttempt().getSubmittedAt() != null) {
            t.put("score", r.getAttempt().getScore());
            t.put("total", r.getAttempt().getTotal());
        }
        return t;
    }

    static Map<String, Object> live(LiveSession s, String materialTitle, List<LiveMessage> msgs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LiveMessage x : msgs) {
            list.add(m("who", x.getAuthorName(), "role", x.getAuthorRole(), "text", x.getText(), "at", x.getCreatedAt() == null ? null : x.getCreatedAt().toString()));
        }
        return m("active", true, "sessionId", s.getId(), "title", s.getTitle(), "classLevel", s.getClassLevel(), "materialId", s.getMaterialId(),
                "materialTitle", materialTitle, "page", s.getCurrentPage(), "messages", list);
    }
}
