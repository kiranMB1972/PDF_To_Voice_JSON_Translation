package com.org.pdfplatform.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue of the free Microsoft Edge "Natural" voices for South Indian reading (plus Hindi, Marathi, Urdu, Indian English)
 * and the SSML helper. The browser's speechSynthesis plays them - no cloud account or key is needed.
 */
@Service
public class AccentControlService {

    public record VoiceProfile(String name, String lang, String gender, String label) { }

    private static VoiceProfile v(String name, String gender, String label) {
        return new VoiceProfile(name, name.substring(0, 5), gender, label);
    }

    /** Real Microsoft Edge Natural voice ids. The page matches them by the short name (Sapna, Gagan ...). South Indian, Hindi, Marathi, Indian English, Spain Spanish and France French only. */
    private static final List<VoiceProfile> VOICES = List.of(
            v("kn-IN-SapnaNeural", "female", "Sapna - Kannada (India)"),
            v("kn-IN-GaganNeural", "male", "Gagan - Kannada (India)"),
            v("ta-IN-PallaviNeural", "female", "Pallavi - Tamil (India)"),
            v("ta-IN-ValluvarNeural", "male", "Valluvar - Tamil (India)"),
            v("te-IN-ShrutiNeural", "female", "Shruti - Telugu (India)"),
            v("te-IN-MohanNeural", "male", "Mohan - Telugu (India)"),
            v("ur-IN-GulNeural", "female", "Gul - Urdu (India)"),
            v("ur-IN-SalmanNeural", "male", "Salman - Urdu (India)"),
            v("ml-IN-SobhanaNeural", "female", "Sobhana - Malayalam (India)"),
            v("ml-IN-MidhunNeural", "male", "Midhun - Malayalam (India)"),
            v("hi-IN-SwaraNeural", "female", "Swara - Hindi (India)"),
            v("hi-IN-MadhurNeural", "male", "Madhur - Hindi (India)"),
            v("mr-IN-AarohiNeural", "female", "Aarohi - Marathi (India)"),
            v("mr-IN-ManoharNeural", "male", "Manohar - Marathi (India)"),
            v("en-IN-NeerjaNeural", "female", "Neerja - English (India)"),
            v("en-IN-PrabhatNeural", "male", "Prabhat - English (India)"),
            v("es-ES-ElviraNeural", "female", "Elvira - Spanish"),
            v("es-ES-AlvaroNeural", "male", "Alvaro - Spanish"),
            v("fr-FR-DeniseNeural", "female", "Denise - French"),
            v("fr-FR-HenriNeural", "male", "Henri - French"));

    public List<VoiceProfile> allVoices() { return VOICES; }

    /** lang may be a full locale ("ta-IN") or just the language ("es" = every Spanish voice). */
    public List<VoiceProfile> voicesFor(String lang) {
        if (lang == null || lang.isBlank()) {
            return VOICES;
        }
        String want = lang.trim().replace('_', '-');
        boolean fullLocale = want.contains("-");
        List<VoiceProfile> out = new ArrayList<>();
        for (VoiceProfile v : VOICES) {
            boolean match = fullLocale ? v.lang().equalsIgnoreCase(want) : v.lang().regionMatches(true, 0, want, 0, want.length()) && v.lang().charAt(want.length()) == '-';
            if (match) {
                out.add(v);
            }
        }
        return out;
    }

    /**
     * Best catalogue voice for a language + gender. Unknown language -> Kannada (the project's home language).
     * (The earlier build returned the Kannada male voice for EVERY language's male request.)
     * The accent argument is kept for compatibility with the earlier signature.
     */
    public String getVoice(String lang, String gender, String accent) {
        String wantGender = "male".equalsIgnoreCase(gender) ? "male" : "female";
        for (VoiceProfile v : VOICES) {
            if (v.lang().equalsIgnoreCase(lang) && v.gender().equals(wantGender)) {
                return v.name();
            }
        }
        for (VoiceProfile v : VOICES) {
            if (v.lang().equals("kn-IN") && v.gender().equals(wantGender)) {
                return v.name();
            }
        }
        return "kn-IN-SapnaNeural";
    }

    /** style: "warm" (default) or "nila" (slower, lower, softer). */
    public String buildSsml(String text, String voice, String style) {
        boolean nila = "nila".equalsIgnoreCase(style);
        String rate = nila ? "88%" : "92%";
        String pitch = nila ? "-2%" : "+6%";
        String lang = voice != null && voice.length() >= 5 ? voice.substring(0, 5) : "kn-IN";
        return "<speak version='1.0' xml:lang='" + lang + "'><voice name='" + xmlEscape(voice)
                + "'><prosody rate='" + rate + "' pitch='" + pitch + "' volume='soft'>"
                + xmlEscape(text) + "</prosody></voice></speak>";
    }

    /** Escapes the five XML characters so user text can never break out of the SSML element. */
    public static String xmlEscape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
