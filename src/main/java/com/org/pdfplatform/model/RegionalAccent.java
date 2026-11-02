package com.org.pdfplatform.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * South Indian regional flavours ONLY. (The earlier builds also offered Gulf, Mumbai/Pune, Mexican, Argentine,
 * Quebec and Belgian accents - removed on request.) An accent shapes the wording of the translation; Edge has no
 * per-city voices, so the voice is always the language's Indian Natural voice.
 * Spanish, French and English have just STANDARD.
 */
public enum RegionalAccent {
    MYSURU("mysuru", "Mysuru", "Mysuru heritage, calm and polite", "Karnataka"),
    BENGALURU("bengaluru", "Bengaluru", "Bengaluru urban, modern", "Karnataka"),
    MANGALURU("mangaluru", "Mangaluru", "Mangaluru coastal Karnataka", "Karnataka"),
    HUBBALLI("hubballi", "Hubballi-Dharwad", "North Karnataka, Dharwad flavour", "Karnataka"),
    CHENNAI("chennai", "Chennai", "Chennai metro, lively", "Tamil Nadu"),
    MADURAI("madurai", "Madurai", "Madurai classical Tamil", "Tamil Nadu"),
    COIMBATORE("coimbatore", "Coimbatore", "Kongu Tamil of Coimbatore", "Tamil Nadu"),
    TIRUNELVELI("tirunelveli", "Tirunelveli", "Nellai Tamil of the deep south", "Tamil Nadu"),
    VIJAYAWADA("vijayawada", "Vijayawada", "Vijayawada, coastal Andhra", "Andhra Pradesh"),
    VISAKHAPATNAM("vizag", "Visakhapatnam", "Vizag, north coastal Andhra", "Andhra Pradesh"),
    HYDERABAD_TELUGU("hyd_telugu", "Hyderabad Telugu", "Hyderabad, Telangana Telugu", "Telangana"),
    WARANGAL("warangal", "Warangal", "Warangal, northern Telangana", "Telangana"),
    KOCHI("kochi", "Kochi", "Kochi, central Kerala", "Kerala"),
    TRIVANDRUM("trivandrum", "Trivandrum", "Thiruvananthapuram, southern Kerala", "Kerala"),
    KOZHIKODE("kozhikode", "Kozhikode", "Malabar Malayalam of Kozhikode", "Kerala"),
    HYDERABAD_URDU("hyderabad_urdu", "Hyderabad Urdu", "Hyderabadi Deccani Urdu", "Deccan"),
    STANDARD("standard", "Standard", "Standard, neutral wording", "Standard");

    private final String code;
    private final String displayName;
    private final String description;
    private final String category;

    RegionalAccent(String code, String displayName, String description, String category) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    /** The BCP-47 tag for speech: always the language's own Indian locale. */
    public String getBcp47ForLanguage(AppLanguage lang) {
        return lang.getBcp47();
    }

    public List<String> voiceLocalesFor(AppLanguage lang) {
        return lang.getVoiceLocales();
    }

    private static final Map<String, RegionalAccent> INDEX = new HashMap<>();

    static {
        for (RegionalAccent a : values()) {
            INDEX.put(a.code, a);
        }
    }

    public static RegionalAccent fromCode(String code) {
        if (code == null || code.isBlank()) {
            return STANDARD;
        }
        RegionalAccent a = INDEX.get(code.trim().toLowerCase());
        return a != null ? a : STANDARD;
    }

    public static List<RegionalAccent> forLanguage(AppLanguage lang) {
        return switch (lang) {
            case KANNADA -> List.of(MYSURU, BENGALURU, MANGALURU, HUBBALLI, STANDARD);
            case TAMIL -> List.of(CHENNAI, MADURAI, COIMBATORE, TIRUNELVELI, STANDARD);
            case TELUGU -> List.of(VIJAYAWADA, VISAKHAPATNAM, HYDERABAD_TELUGU, WARANGAL, STANDARD);
            case MALAYALAM -> List.of(KOCHI, TRIVANDRUM, KOZHIKODE, STANDARD);
            case URDU -> List.of(HYDERABAD_URDU, STANDARD);
            default -> List.of(STANDARD);
        };
    }
}
