package com.org.pdfplatform.translate;

import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import org.springframework.stereotype.Component;

/** Builds the instruction sent to the local model. Generic: nothing is hard-wired to one textbook. */
@Component
public class PromptBuilder {

    public enum Mode {
        /** Faithful translation. */
        TRANSLATE,
        /** Translate, then add a short plain-language explanation of difficult words. */
        TEACH,
        /** Translate in a smooth, easy-to-listen-to style for reading aloud. */
        ORATE;

        public static Mode of(String s) {
            if (s == null || s.isBlank()) {
                return TRANSLATE;
            }
            try {
                return valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return TRANSLATE;
            }
        }
    }

    public String build(String text, AppLanguage target, RegionalAccent accent, Mode mode, String sourceLanguageName) {
        StringBuilder p = new StringBuilder();
        p.append("You are a careful professional translator.\n");
        p.append("Translate the text between <text> and </text> from ").append(sourceLanguageName)
                .append(" into ").append(target.getEnglishName()).append(" (").append(target.getNativeName()).append(").\n");
        p.append("Rules:\n");
        p.append("- Use ONLY the given text. Do not add, remove or invent anything.\n");
        p.append("- Keep names, numbers, headings and paragraph breaks.\n");
        p.append("- Write the answer in the ").append(target.getEnglishName()).append(" script only.\n");
        if (accent != null && accent != RegionalAccent.STANDARD) {
            p.append("- Prefer wording natural to ").append(accent.getDisplayName()).append(" (").append(accent.getDescription()).append(").\n");
        }
        switch (mode) {
            case TEACH -> p.append("- After the translation add a short section 'Words to learn' that explains 3 to 5 harder words simply, in ")
                    .append(target.getEnglishName()).append(".\n");
            case ORATE -> p.append("- Make the sentences smooth and easy to listen to when read aloud; avoid brackets and symbols.\n");
            default -> p.append("- Give the translation only, with no comments.\n");
        }
        p.append("\n<text>\n").append(text).append("\n</text>\n");
        return p.toString();
    }

    /** Removes wrappers small models like to add (tags, code fences, "Here is the translation:"). */
    public String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.strip();
        s = s.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("(?s)\\s*```$", "");
        s = s.replaceAll("(?is)^\\s*<text>\\s*", "").replaceAll("(?is)\\s*</text>\\s*$", "");
        s = s.replaceFirst("(?i)^(here is|here's|sure[,!]?|translation:)[^\\n]{0,80}:\\s*\\n+", "");
        return s.strip();
    }
}
