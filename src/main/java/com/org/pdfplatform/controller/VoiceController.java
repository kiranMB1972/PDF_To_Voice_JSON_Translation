package com.org.pdfplatform.controller;

import com.org.pdfplatform.config.NilaPrivateVoiceConfig;
import com.org.pdfplatform.service.AccentControlService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Voice catalogue + descriptors. The actual speaking is done free in the browser (Edge Natural voices).
 * /sarvam and /nila-private keep the exact paths of the earlier builds.
 */
@RestController
@RequestMapping("/api/v1/voice")
public class VoiceController {

    private final AccentControlService accents;
    private final NilaPrivateVoiceConfig nila;
    private final String sarvamKey;
    private final String azureKey;

    public VoiceController(AccentControlService accents, NilaPrivateVoiceConfig nila,
                           @Value("${sarvam.api-key:}") String sarvamKey,
                           @Value("${azure.speech.key:}") String azureKey) {
        this.accents = accents;
        this.nila = nila;
        this.sarvamKey = sarvamKey;
        this.azureKey = azureKey;
    }

    @GetMapping("/health")
    public String health() {
        return "PDF VOICE AND GAMES RUNNING - South Indian voices + Nila Private";
    }

    @GetMapping("/voices")
    public Object voices(@RequestParam(required = false) String lang) {
        return accents.voicesFor(lang);
    }

    @GetMapping("/ssml")
    public Map<String, String> ssml(@RequestParam String text,
                                    @RequestParam(defaultValue = "kn-IN") String lang,
                                    @RequestParam(defaultValue = "female") String gender,
                                    @RequestParam(defaultValue = "warm") String style) {
        String voice = accents.getVoice(lang, gender, style);
        return Map.of("voice", voice, "ssml", accents.buildSsml(text, voice, style));
    }

    @GetMapping("/sarvam")
    public Map<String, Object> sarvam(@RequestParam String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("voice", "bulbul-female-attractive-warm");
        m.put("text", text);
        m.put("enabled", !sarvamKey.isBlank());      // true only when SARVAM_API_KEY is set
        m.put("note", "Descriptor only - the free browser voices do the speaking.");
        return m;
    }

    @GetMapping("/nila-private")
    public Map<String, Object> nilaPrivate(@RequestParam String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("voice", nila.getVoice());
        m.put("engine", "kn-IN-SapnaNeural soft warm private South Indian");
        m.put("text", text);
        m.put("azureEnabled", !azureKey.isBlank());
        m.put("ssml", nila.getAzureSSML(text));
        return m;
    }
}
