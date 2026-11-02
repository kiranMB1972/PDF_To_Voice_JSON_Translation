package com.org.pdfplatform.controller;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.RequestRejectedException;
import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Language and accent lists for the page's dropdowns (DTOs - the earlier build returned bare enums: "undefined"). */
@RestController
@RequestMapping("/api")
public class AccentApiController {

    public record LanguageDto(String code, String bcp47, String englishName, String nativeName, String family,
                              String familyLabel, List<String> voiceLocales) { }

    public record AccentDto(String code, String displayName, String description, String category, String bcp47,
                            List<String> voiceLocales) { }

    @GetMapping("/languages")
    public List<LanguageDto> languages() {
        return Arrays.stream(AppLanguage.values())
                .map(l -> new LanguageDto(l.getCode(), l.getBcp47(), l.getEnglishName(), l.getNativeName(), l.getFamily().name(), l.getFamilyLabel(), l.getVoiceLocales()))
                .toList();
    }

    @GetMapping("/accents")
    public List<AccentDto> accents(@RequestParam String langCode) {
        AppLanguage lang = AppLanguage.find(langCode).orElseThrow(() -> new RequestRejectedException(
                ErrorCodeModel.INVALID_LANGUAGE, "Unknown language: " + langCode, "Choose a language from the list."));
        return RegionalAccent.forLanguage(lang).stream()
                .map(a -> new AccentDto(a.getCode(), a.getDisplayName(), a.getDescription(), a.getCategory(),
                        a.getBcp47ForLanguage(lang), a.voiceLocalesFor(lang)))
                .toList();
    }

    @GetMapping("/greeting")
    public Map<String, String> greeting() {
        return Map.of("greeting", greetingFor(LocalTime.now().getHour()));
    }

    static String greetingFor(int hour) {
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        if (hour < 20) return "Good evening";
        return "Good night";
    }
}
