package com.org.pdfplatform.school.testing;

import java.util.Map;
import java.util.Set;

/** Exact-match marking: a question scores its marks only when the ticked boxes equal the correct set. */
public final class Grader {

    public record Result(int score, int total, boolean[] correctPerQuestion) { }

    private Grader() { }

    /** @param answers question number (1-based) to the letters the student ticked */
    public static Result grade(TestSpec spec, Map<Integer, Set<String>> answers) {
        int score = 0;
        int total = 0;
        boolean[] ok = new boolean[spec.questions().size()];
        for (int i = 0; i < spec.questions().size(); i++) {
            QuestionSpec q = spec.questions().get(i);
            total += q.marks();
            Set<String> given = answers.get(i + 1);
            if (given != null && given.equals(q.correct())) {
                score += q.marks();
                ok[i] = true;
            }
        }
        return new Result(score, total, ok);
    }
}
