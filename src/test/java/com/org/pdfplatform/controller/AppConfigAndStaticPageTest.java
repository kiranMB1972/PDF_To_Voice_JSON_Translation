package com.org.pdfplatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppConfigAndStaticPageTest {

    private static String read(String path) throws Exception {
        try (InputStream in = AppConfigAndStaticPageTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing classpath resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void configTellsThePageWhereTheGamesMicroserviceIs() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AppConfigController("http://localhost:8095/")).build();
        mvc.perform(get("/api/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesServiceUrl").value("http://localhost:8095"));      // trailing slash removed
    }

    @Test
    void homePageHasClockCompassGamesDropDownAndAllTheReaderControls() throws Exception {
        String html = read("/static/index.html");
        for (String id : new String[]{"clock", "compass", "compassRose", "gamesSel", "pdfFile", "targetSel", "accentSel", "voiceSel",
                "readingPreview", "origPanel", "progressBar", "playBtn", "sampleBtn", "checkBtn"}) {
            assertTrue(html.contains("id=\"" + id + "\""), id);
        }
        assertTrue(html.contains("Entertainment Games"));
        assertTrue(html.indexOf("id=\"clock\"") < html.indexOf("id=\"gamesSel\""), "clock (top-left) comes before the games menu (top-right)");
        assertTrue(html.contains("js/hud.js") && html.contains("js/games-menu.js") && html.contains("js/speech.js"));
    }

    @Test
    void themeIsDarkSkyBlueWithNoBlackAnywhere() throws Exception {
        for (String f : new String[]{"/static/css/gold.css", "/static/index.html"}) {
            String t = read(f).toLowerCase();
            assertFalse(t.matches("(?s).*#000(000)?\\b.*"), f + " must not use pure black");
            assertFalse(t.contains("rgba(0,0,0") || t.contains("rgba(0, 0, 0") || t.contains("background:black") || t.contains(": black"), f);
            assertFalse(t.contains("#0f0f0f") || t.contains("#111") || t.contains("#1e1e1e"), f);
        }
        assertTrue(read("/static/css/gold.css").contains("--sky-1:#0b3a68"));
    }

    @Test
    void gamesAreNoLongerPartOfTheMainApp() {
        assertNull(AppConfigAndStaticPageTest.class.getResourceAsStream("/static/games/common.js"));
        assertNull(AppConfigAndStaticPageTest.class.getResourceAsStream("/static/js/games-corner.js"));
    }
}
