package com.org.pdfplatform.config;

import com.org.pdfplatform.service.AccentControlService;
import org.springframework.stereotype.Component;

/** "Nila - Private Reading Mode": a slower, softer, lower-pitched South Indian reading style. */
@Component
public class NilaPrivateVoiceConfig {

    public static final String LABEL = "Nila - Private Reading Mode - South Indian Only";
    public static final String VOICE = "kn-IN-SapnaNeural";

    public String getVoice() { return LABEL; }

    public String getSarvamVoice() { return "bulbul-female-warm-private"; }

    /** Same SSML as the earlier builds, but the text is XML-escaped now (it was injected raw before). */
    public String getAzureSSML(String text) {
        return "<speak version='1.0' xml:lang='kn-IN'><voice name='" + VOICE
                + "'><prosody rate='88%' pitch='-2%' volume='soft'>"
                + AccentControlService.xmlEscape(text) + "</prosody></voice></speak>";
    }
}
