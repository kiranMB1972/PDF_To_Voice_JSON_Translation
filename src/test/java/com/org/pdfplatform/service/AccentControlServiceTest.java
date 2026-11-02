package com.org.pdfplatform.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccentControlServiceTest {

    private final AccentControlService svc = new AccentControlService();

    @Test
    void catalogueHasEightVoicesOfEachKindAndEveryNameFollowsTheEdgeScheme() {
        assertEquals(20, svc.allVoices().size());
        assertEquals(8, svc.allVoices().stream().filter(v -> v.gender().equals("female")).count());
        assertEquals(8, svc.allVoices().stream().filter(v -> v.gender().equals("male")).count());
        for (var v : svc.allVoices()) {
            assertTrue(v.name().matches("[a-z]{2}-[A-Z]{2}-[A-Za-z]+Neural"), v.name());
            assertTrue(v.name().startsWith(v.lang() + "-"), v.name());
        }
        assertEquals(16, svc.allVoices().stream().map(v -> v.name()).distinct().count());
    }

    @Test
    void onlySouthIndianIndianEnglishAndTheTwoInternationalLocalesAreOffered() {       // no Pakistan, Gulf, Mexico, Canada ... voices
        var allowed = java.util.Set.of("kn-IN", "ta-IN", "te-IN", "ur-IN", "ml-IN", "en-IN", "es-ES", "fr-FR");
        for (var v : svc.allVoices()) {
            assertTrue(allowed.contains(v.lang()), "offshore/unrequested voice: " + v.name());
        }
    }

    @Test
    void uiLabelsNeverUseGenderWords() {
        for (var v : svc.allVoices()) {
            String l = v.label().toLowerCase();
            assertFalse(l.contains("female") || l.contains("male") || l.contains("human"), v.label());
        }
    }

    @Test
    void femaleAndMalePicksFollowTheLanguage() {
        assertEquals("kn-IN-SapnaNeural", svc.getVoice("kn-IN", "female", "x"));
        assertEquals("ta-IN-PallaviNeural", svc.getVoice("ta-IN", "female", ""));
        assertEquals("te-IN-ShrutiNeural", svc.getVoice("te-IN", "female", ""));
        assertEquals("ml-IN-SobhanaNeural", svc.getVoice("ml-IN", "female", ""));
        assertEquals("ur-IN-GulNeural", svc.getVoice("ur-IN", "female", ""));
        assertEquals("es-ES-AlvaroNeural", svc.getVoice("es-ES", "male", ""));
        assertEquals("fr-FR-HenriNeural", svc.getVoice("fr-FR", "male", ""));
        assertEquals("ta-IN-ValluvarNeural", svc.getVoice("ta-IN", "male", ""));          // old build: always the Kannada male voice
    }

    @Test
    void unknownLanguageFallsBackToKannada() {
        assertEquals("kn-IN-SapnaNeural", svc.getVoice("xx-XX", "female", ""));
        assertEquals("kn-IN-GaganNeural", svc.getVoice(null, "male", ""));
        assertEquals("hi-IN-SwaraNeural", svc.getVoice("hi-IN", "female", ""));
        assertEquals("mr-IN-ManoharNeural", svc.getVoice("mr-IN", "male", ""));
        assertEquals("kn-IN-SapnaNeural", svc.getVoice("pa-IN", "female", ""));
    }

    @Test
    void voicesForFiltersByLocaleOrByLanguage() {
        assertEquals(2, svc.voicesFor("ta-IN").size());
        assertEquals(2, svc.voicesFor("es").size());
        assertEquals(2, svc.voicesFor("fr").size());
        assertEquals(2, svc.voicesFor("ur").size());
        assertEquals(2, svc.voicesFor("hi").size());
        assertEquals(2, svc.voicesFor("mr").size());
        assertEquals(0, svc.voicesFor("ur-PK").size());
        assertEquals(20, svc.voicesFor("").size());
    }

    @Test
    void ssmlEscapesUserTextSoItCannotInjectMarkup() {
        String ssml = svc.buildSsml("a <b> & \"c\" </prosody><x/>", "kn-IN-SapnaNeural", "warm");
        assertTrue(ssml.contains("a &lt;b&gt; &amp; &quot;c&quot; &lt;/prosody&gt;&lt;x/&gt;"));
        assertFalse(ssml.contains("<x/>"));
        assertTrue(ssml.startsWith("<speak version='1.0' xml:lang='kn-IN'>"));
    }

    @Test
    void nilaStyleIsSlowerAndLowerThanWarm() {
        String warm = svc.buildSsml("hi", "kn-IN-SapnaNeural", "warm");
        String nila = svc.buildSsml("hi", "kn-IN-SapnaNeural", "nila");
        assertTrue(warm.contains("rate='92%'") && warm.contains("pitch='+6%'"));
        assertTrue(nila.contains("rate='88%'") && nila.contains("pitch='-2%'"));
    }

    @Test
    void xmlEscapeHandlesNull() {
        assertEquals("", AccentControlService.xmlEscape(null));
    }
}
