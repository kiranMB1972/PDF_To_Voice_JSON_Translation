package com.org.pdfplatform.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorPageRendererTest {

    private static ErrorPageRenderer.PageModel model(String code, String message) {
        ErrorGuide.Guide g = ErrorGuide.forCode(code);
        return new ErrorPageRenderer.PageModel(404, code, g, message, "Use the Choose file button", "Analyze PDF", "POST", "/api/v1/x?<b>", "ERR-20261031-ABCD", "2026-10-31 12:00:00");
    }

    @Test
    void theHdPageShowsReasonStepsIdeaContextAndReference() {
        String html = ErrorPageRenderer.render(model("SCHOOL_DB_OFFLINE", "no route to host"));
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("The school database is not reachable"));
        assertTrue(html.contains("no route to host"));
        assertTrue(html.contains("What you were doing"));
        assertTrue(html.contains("Analyze PDF"));
        assertTrue(html.contains("How to fix it"));
        assertTrue(html.contains("Out-of-the-box idea"));
        assertTrue(html.contains("ERR-20261031-ABCD"));
        assertTrue(html.contains("@keyframes"), "animated");
        assertTrue(html.contains("prefers-reduced-motion"), "respects reduced motion");
    }

    @Test
    void everyTextFromTheRequestIsEscapedSoAnAddressCannotInjectScript() {
        String html = ErrorPageRenderer.render(model("NOT_FOUND", "<script>alert(1)</script> & \"q\""));
        assertFalse(html.contains("<script>alert(1)"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt; &amp; &quot;q&quot;"));
        assertTrue(html.contains("/api/v1/x?&lt;b&gt;"));
    }

    @Test
    void theColourThemeAndSceneFollowTheCategoryAndEveryCategoryHasAnIllustration() {
        assertTrue(ErrorPageRenderer.render(model("NO_VOICE", "x")).contains("--c1:#f7971e"));
        assertTrue(ErrorPageRenderer.render(model("NETWORK", "x")).contains("--c1:#0f9b8e"));
        for (ErrorGuide.Guide g : ErrorGuide.all().values()) {
            String scene = ErrorPageRenderer.scene(g.category());
            assertTrue(scene.startsWith("<svg") && scene.endsWith("</svg>"), g.category());
        }
    }

    @Test
    void thePageIsSelfContainedSoItAlsoWorksWhenTheRestOfTheAppIsBroken() {
        for (ErrorGuide.Guide g : ErrorGuide.all().values()) {
            String html = ErrorPageRenderer.render(model(g.code(), "m"));
            assertFalse(html.contains("<link"), g.code());
            assertFalse(html.contains("src=\"http"), g.code());
            assertFalse(html.contains("url(http"), g.code());
            assertEquals(1, html.split("<h1>", -1).length - 1, g.code());
        }
    }

    @Test
    void escapeHandlesNullAndQuotes() {
        assertEquals("", ErrorPageRenderer.esc(null));
        assertEquals("&#39;a&#39;", ErrorPageRenderer.esc("'a'"));
    }
}
