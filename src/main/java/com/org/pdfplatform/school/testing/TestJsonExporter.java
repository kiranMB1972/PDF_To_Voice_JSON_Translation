package com.org.pdfplatform.school.testing;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The same test as Test&lt;DD-MM-YYYY&gt;.properties, written as JSON (Test&lt;DD-MM-YYYY&gt;.json). Dependency-free on purpose. */
public final class TestJsonExporter {

    private TestJsonExporter() { }

    /** @param withAnswers false = the student-safe version (no "correct" lists) */
    public static String toJson(TestSpec t, String fileName, boolean withAnswers) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        field(sb, 1, "file", str(fileName), true);
        field(sb, 1, "title", str(t.title()), true);
        field(sb, 1, "date", str(DateTimeFormatter.ISO_LOCAL_DATE.format(t.date())), true);
        field(sb, 1, "dateDMY", str(TestFileNames.DMY.format(t.date())), true);
        field(sb, 1, "classLevel", String.valueOf(t.classLevel()), true);
        field(sb, 1, "subject", str(t.subject()), true);
        field(sb, 1, "durationMinutes", String.valueOf(t.durationMinutes()), true);
        field(sb, 1, "materialId", t.materialId() == null ? "null" : String.valueOf(t.materialId()), true);
        field(sb, 1, "totalMarks", String.valueOf(t.totalMarks()), true);
        List<String> ids = new ArrayList<>();
        for (Long id : t.studentIds()) {
            ids.add(String.valueOf(id));
        }
        field(sb, 1, "studentIds", "[" + String.join(", ", ids) + "]", true);
        sb.append("  \"questions\": [\n");
        for (int i = 0; i < t.questions().size(); i++) {
            QuestionSpec q = t.questions().get(i);
            sb.append("    {\n");
            field(sb, 3, "number", String.valueOf(i + 1), true);
            field(sb, 3, "text", str(q.text()), true);
            field(sb, 3, "marks", String.valueOf(q.marks()), true);
            field(sb, 3, "multipleAnswers", String.valueOf(q.correct().size() > 1), true);
            List<String> opts = new ArrayList<>();
            for (int k = 0; k < q.options().size(); k++) {
                opts.add("{\"letter\": " + str(QuestionSpec.letter(k)) + ", \"text\": " + str(q.options().get(k)) + "}");
            }
            field(sb, 3, "options", "[" + String.join(", ", opts) + "]", withAnswers);
            if (withAnswers) {
                List<String> cs = new ArrayList<>();
                for (String c : new java.util.TreeSet<>(q.correct())) {
                    cs.add(str(c));
                }
                field(sb, 3, "correct", "[" + String.join(", ", cs) + "]", false);
            }
            sb.append("    }").append(i < t.questions().size() - 1 ? "," : "").append('\n');
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    private static void field(StringBuilder sb, int indent, String key, String rawValue, boolean comma) {
        sb.append("  ".repeat(indent)).append('"').append(key).append("\": ").append(rawValue).append(comma ? "," : "").append('\n');
    }

    /** JSON string literal with escapes. */
    public static String str(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }
}
