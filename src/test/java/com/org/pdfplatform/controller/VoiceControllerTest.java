package com.org.pdfplatform.controller;

import com.org.pdfplatform.config.NilaPrivateVoiceConfig;
import com.org.pdfplatform.service.AccentControlService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VoiceControllerTest {

    private final MockMvc voice = MockMvcBuilders.standaloneSetup(
            new VoiceController(new AccentControlService(), new NilaPrivateVoiceConfig(), "", "")).build();

    @Test
    void healthEndpointKeepsItsPath() throws Exception {
        voice.perform(get("/api/v1/voice/health")).andExpect(status().isOk())
                .andExpect(content().string(containsString("RUNNING")));
    }

    @Test
    void voicesCanBeFilteredByLanguage() throws Exception {
        voice.perform(get("/api/v1/voice/voices").param("lang", "ml-IN")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("ml-IN-SobhanaNeural"));
    }

    @Test
    void sarvamDescriptorIsDisabledWithoutAKey() throws Exception {
        voice.perform(get("/api/v1/voice/sarvam").param("text", "hi")).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.voice").value("bulbul-female-attractive-warm"));
    }

    @Test
    void nilaPrivateEscapesTheTextInItsSsml() throws Exception {
        voice.perform(get("/api/v1/voice/nila-private").param("text", "<script>x</script>")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ssml").value(not(containsString("<script>"))));
    }
}
