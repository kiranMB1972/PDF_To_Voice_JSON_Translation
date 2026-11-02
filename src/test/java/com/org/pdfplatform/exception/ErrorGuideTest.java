package com.org.pdfplatform.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorGuideTest {

    @Test
    void everyServerErrorCodeHasACompleteGuide() {
        for (ErrorCodeModel c : ErrorCodeModel.values()) {
            ErrorGuide.Guide g = ErrorGuide.forCode(c);
            assertEquals(c.name(), g.code(), "no own guide for " + c);
            assertFalse(g.headline().isBlank(), c + " headline");
            assertFalse(g.reason().isBlank(), c + " reason");
            assertFalse(g.steps().isEmpty(), c + " steps");
            assertTrue(g.color1().matches("#[0-9a-fA-F]{6}") && g.color2().matches("#[0-9a-fA-F]{6}"), c + " colours");
        }
    }

    @Test
    void theBrowserOnlyCodesAreCataloguedToo() {
        for (String c : new String[]{"NETWORK", "NO_VOICE", "NO_TEXT", "EDGE", "CHOOSE_LANGUAGE", "TRANSLATION_FAILED", "TRANSLATION_OK", "INFO"}) {
            assertEquals(c, ErrorGuide.forCode(c).code());
        }
    }

    @Test
    void anUnknownCodeFallsBackToTheGenericServerGuideAndNeverNull() {
        assertEquals("INTERNAL_ERROR", ErrorGuide.forCode("DOES_NOT_EXIST").code());
        assertEquals("INTERNAL_ERROR", ErrorGuide.forCode((String) null).code());
    }

    @Test
    void stepsDoNotRepeatEachOther() {
        for (ErrorGuide.Guide g : ErrorGuide.all().values()) {
            assertEquals(g.steps().size(), g.steps().stream().distinct().count(), g.code() + " repeats a step");
        }
    }

    @Test
    void httpStatusesMapToTheRightGuide() {
        assertEquals(ErrorCodeModel.NOT_FOUND, ErrorGuide.codeForStatus(404));
        assertEquals(ErrorCodeModel.METHOD_NOT_ALLOWED, ErrorGuide.codeForStatus(405));
        assertEquals(ErrorCodeModel.AUTH_REQUIRED, ErrorGuide.codeForStatus(401));
        assertEquals(ErrorCodeModel.ACCESS_DENIED, ErrorGuide.codeForStatus(403));
        assertEquals(ErrorCodeModel.FILE_TOO_LARGE, ErrorGuide.codeForStatus(413));
        assertEquals(ErrorCodeModel.INTERNAL_ERROR, ErrorGuide.codeForStatus(500));
        assertEquals(ErrorCodeModel.INTERNAL_ERROR, ErrorGuide.codeForStatus(502));
    }

    @Test
    void theOperationIsNamedInPlainWords() {
        assertEquals("Analyze PDF", ErrorGuide.operationFor("POST", "/api/v1/documents/analyze-sync"));
        assertEquals("Test translation", ErrorGuide.operationFor("GET", "/api/v1/translate/selftest"));
        assertEquals("Translate text", ErrorGuide.operationFor("POST", "/api/v1/translate"));
        assertEquals("Log in", ErrorGuide.operationFor("POST", "/school/api/auth/login"));
        assertEquals("Take a test", ErrorGuide.operationFor("GET", "/school/api/student/tests/3/start"));
        assertEquals("Opening /nothing", ErrorGuide.operationFor("GET", "/nothing"));
        assertNotNull(ErrorGuide.operationFor("GET", null));
    }
}
