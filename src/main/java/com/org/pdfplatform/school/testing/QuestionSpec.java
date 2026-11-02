package com.org.pdfplatform.school.testing;

import java.util.List;
import java.util.Set;

/** One multiple-choice (check box) question. Options are lettered A, B, C ... in list order. */
public record QuestionSpec(String text, List<String> options, Set<String> correct, int marks) {

    public static String letter(int index) {
        return String.valueOf((char) ('A' + index));
    }
}
