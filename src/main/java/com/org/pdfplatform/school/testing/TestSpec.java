package com.org.pdfplatform.school.testing;

import java.time.LocalDate;
import java.util.List;

/** Everything a live test screen needs. Stored as Test&lt;DD-MM-YYYY&gt;.properties. */
public record TestSpec(String title, int classLevel, String subject, LocalDate date, int durationMinutes,
                       Long materialId, List<Long> studentIds, List<QuestionSpec> questions) {

    public int totalMarks() {
        int t = 0;
        for (QuestionSpec q : questions) {
            t += q.marks();
        }
        return t;
    }
}
