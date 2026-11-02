package com.org.pdfplatform.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The languages offered in the drop-down: the South Indian languages (plus Urdu, the Deccan language), Hindi and Marathi,
 * English (so Indian-language PDFs can also be read in English) and the two International Languages, Spanish and French.
 * Locales are the Edge Natural voice locales, best first. Only Indian locales are used for Indian languages (Hindi and Marathi included) and English
 * (no Pakistani, Gulf or other overseas voices).
 */
public enum AppLanguage {
    KANNADA("kn", "kn-IN", "Kannada", "\u0C95\u0CA8\u0CCD\u0CA8\u0CA1", Family.SOUTH_INDIAN, List.of("kn-IN")),
    TAMIL("ta", "ta-IN", "Tamil", "\u0BA4\u0BAE\u0BBF\u0BB4\u0BCD", Family.SOUTH_INDIAN, List.of("ta-IN")),
    TELUGU("te", "te-IN", "Telugu", "\u0C24\u0C46\u0C32\u0C41\u0C17\u0C41", Family.SOUTH_INDIAN, List.of("te-IN")),
    URDU("ur", "ur-IN", "Urdu", "\u0627\u0631\u062F\u0648", Family.SOUTH_INDIAN, List.of("ur-IN")),
    MALAYALAM("ml", "ml-IN", "Malayalam", "\u0D2E\u0D32\u0D2F\u0D3E\u0D33\u0D02", Family.SOUTH_INDIAN, List.of("ml-IN")),
    HINDI("hi", "hi-IN", "Hindi", "\u0939\u093F\u0928\u094D\u0926\u0940", Family.OTHER_INDIAN, List.of("hi-IN")),
    MARATHI("mr", "mr-IN", "Marathi", "\u092E\u0930\u093E\u0920\u0940", Family.OTHER_INDIAN, List.of("mr-IN")),
    ENGLISH("en", "en-IN", "English", "English", Family.ENGLISH, List.of("en-IN")),
    SPANISH("es", "es-ES", "Spanish", "Espa\u00F1ol", Family.INTERNATIONAL, List.of("es-ES")),
    FRENCH("fr", "fr-FR", "French", "Fran\u00E7ais", Family.INTERNATIONAL, List.of("fr-FR"));

    /** The group shown in the drop-down. */
    public enum Family {
        SOUTH_INDIAN("South Indian Languages"),
        OTHER_INDIAN("Other Indian Languages"),
        ENGLISH("English (Indian PDF text to English)"),
        INTERNATIONAL("International Languages");

        private final String label;

        Family(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    private final String code;
    private final String bcp47;
    private final String englishName;
    private final String nativeName;
    private final Family family;
    private final List<String> voiceLocales;

    AppLanguage(String code, String bcp47, String englishName, String nativeName, Family family, List<String> voiceLocales) {
        this.code = code;
        this.bcp47 = bcp47;
        this.englishName = englishName;
        this.nativeName = nativeName;
        this.family = family;
        this.voiceLocales = voiceLocales;
    }

    public String getCode() { return code; }
    public String getBcp47() { return bcp47; }
    public String getEnglishName() { return englishName; }
    public String getNativeName() { return nativeName; }
    public Family getFamily() { return family; }
    public String getFamilyLabel() { return family.getLabel(); }
    public List<String> getVoiceLocales() { return voiceLocales; }
    public boolean isSouthIndian() { return family == Family.SOUTH_INDIAN; }

    private static final Map<String, AppLanguage> INDEX = new HashMap<>();

    static {
        for (AppLanguage l : values()) {
            INDEX.put(l.code, l);
            INDEX.put(l.englishName.toLowerCase(), l);
            INDEX.put(l.bcp47.toLowerCase(), l);
        }
    }

    /** Strict lookup: "kn", "Kannada" or "kn-IN" (any case). */
    public static Optional<AppLanguage> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(INDEX.get(code.trim().toLowerCase()));
    }

    /** Lenient lookup: unknown/blank means ENGLISH (the earlier build silently turned unknown codes into Telugu). */
    public static AppLanguage fromCode(String code) {
        return find(code).orElse(ENGLISH);
    }
}
