package com.org.pdfplatform.translate;

/**
 * Guesses the language of a text from its writing system. Reliable for the Indic scripts and Arabic/Urdu script;
 * Latin text is reported as English (Spanish/French cannot be told apart from English this cheaply).
 */
public final class ScriptDetector {

    private ScriptDetector() { }

    /** @return kn, ta, te, ml, mr, hi, ur or en */
    public static String detect(String text) {
        if (text == null || text.isBlank()) {
            return "en";
        }
        int kn = 0, te = 0, ta = 0, ml = 0, dev = 0, ar = 0, latin = 0, marathiMarks = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0C80 && c <= 0x0CFF) kn++;
            else if (c >= 0x0C00 && c <= 0x0C7F) te++;
            else if (c >= 0x0B80 && c <= 0x0BFF) ta++;
            else if (c >= 0x0D00 && c <= 0x0D7F) ml++;
            else if (c >= 0x0900 && c <= 0x097F) {
                dev++;
                if (c == 0x0933) marathiMarks++;              // the letter LLA is common in Marathi, rare in Hindi
            } else if ((c >= 0x0600 && c <= 0x06FF) || (c >= 0x0750 && c <= 0x077F) || (c >= 0xFB50 && c <= 0xFDFF) || (c >= 0xFE70 && c <= 0xFEFF)) ar++;
            else if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) latin++;
        }
        int total = kn + te + ta + ml + dev + ar + latin;
        if (total == 0) {
            return "en";
        }
        int best = Math.max(Math.max(Math.max(kn, te), Math.max(ta, ml)), Math.max(Math.max(dev, ar), latin));
        if (best == latin) return "en";
        if (best == kn) return "kn";
        if (best == te) return "te";
        if (best == ta) return "ta";
        if (best == ml) return "ml";
        if (best == ar) return "ur";
        return marathiMarks * 200 >= dev ? "mr" : "hi";          // >= 0.5 % LLA letters -> Marathi
    }

    /**
     * Is this really a translation into the target language? A small local model sometimes answers in English,
     * transliterates, or just repeats the input; such an answer must not be shown or spoken as "Hindi" etc.
     * Indic and Urdu targets must come back in the right script; Spanish/French can only be checked for "not unchanged".
     */
    public static boolean outputMatchesTarget(String input, String output, String targetCode) {
        if (output == null || output.isBlank()) {
            return false;
        }
        if (input != null && input.strip().equalsIgnoreCase(output.strip())) {
            return false;
        }
        String got = detect(output);
        return switch (targetCode) {
            case "kn", "ta", "te", "ml", "ur", "en" -> got.equals(targetCode);
            case "hi", "mr" -> got.equals("hi") || got.equals("mr");
            default -> true;
        };
    }
}
