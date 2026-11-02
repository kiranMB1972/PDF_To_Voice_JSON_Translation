package com.org.pdfplatform.translate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptDetectorTest {

    @Test
    void detectsEachSouthIndianScript() {
        assertEquals("kn", ScriptDetector.detect("\u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0 \u0CAE\u0CC8\u0CB8\u0CC2\u0CB0\u0CC1"));
        assertEquals("ta", ScriptDetector.detect("\u0BB5\u0BA3\u0B95\u0BCD\u0B95\u0BAE\u0BCD \u0B9A\u0BC6\u0BA9\u0BCD\u0BA9\u0BC8"));
        assertEquals("te", ScriptDetector.detect("\u0C28\u0C2E\u0C38\u0C4D\u0C15\u0C3E\u0C30\u0C02 \u0C39\u0C48\u0C26\u0C30\u0C3E\u0C2C\u0C3E\u0C26\u0C4D"));
        assertEquals("ml", ScriptDetector.detect("\u0D28\u0D2E\u0D38\u0D4D\u0D15\u0D3E\u0D30\u0D02 \u0D15\u0D4A\u0D1A\u0D4D\u0D1A\u0D3F"));
    }

    @Test
    void urduIsArabicScript() {
        assertEquals("ur", ScriptDetector.detect("\u0622\u067E \u06A9\u06CC\u0633\u06D2 \u06C1\u06CC\u06BA"));
    }

    @Test
    void devanagariIsHindiUnlessMarathiLettersAppear() {
        assertEquals("hi", ScriptDetector.detect("\u0928\u092E\u0938\u094D\u0924\u0947 \u0926\u094B\u0938\u094D\u0924\u094B\u0902 \u0906\u092A \u0915\u0948\u0938\u0947 \u0939\u0948\u0902"));
        assertEquals("mr", ScriptDetector.detect("\u0936\u093E\u0933\u0947\u0924 \u092E\u0941\u0932\u0947 \u0916\u0947\u0933\u0924\u093E\u0924 \u0935\u0933\u0923 \u0915\u0933\u0940"));
    }

    @Test
    void latinOrEmptyIsEnglishAndMixedTextFollowsTheMajority() {
        assertEquals("en", ScriptDetector.detect("School Education Department"));
        assertEquals("en", ScriptDetector.detect(""));
        assertEquals("en", ScriptDetector.detect(null));
        assertEquals("en", ScriptDetector.detect("12345 !!!"));
        assertEquals("kn", ScriptDetector.detect("Page 1 \u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0 \u0CAE\u0CC8\u0CB8\u0CC2\u0CB0\u0CC1 \u0CB6\u0CBE\u0CB2\u0CC6 \u0CAA\u0CC1\u0CB8\u0CCD\u0CA4\u0C95"));
    }

    @Test
    void outputMustBeInTheTargetScriptAndNotTheUnchangedInput() {
        String hindi = "\u0938\u0941\u092A\u094D\u0930\u092D\u093E\u0924\u0964 \u0906\u091C \u0939\u092E \u092A\u0947\u0921\u093C\u094B\u0902 \u0915\u0947 \u092C\u093E\u0930\u0947 \u092E\u0947\u0902 \u0938\u0940\u0916\u0947\u0902\u0917\u0947\u0964";
        assertTrue(ScriptDetector.outputMatchesTarget("Good morning", hindi, "hi"));
        assertTrue(ScriptDetector.outputMatchesTarget("Good morning", hindi, "mr"));
        assertFalse(ScriptDetector.outputMatchesTarget("Good morning", "Good morning", "hi"));
        assertFalse(ScriptDetector.outputMatchesTarget("Good morning", "Suprabhat bachchon", "hi"));
        assertFalse(ScriptDetector.outputMatchesTarget("Good morning", hindi, "kn"));
        assertTrue(ScriptDetector.outputMatchesTarget("Good morning", "Buenos d\u00EDas", "es"));
        assertFalse(ScriptDetector.outputMatchesTarget("Good morning", "  ", "fr"));
    }
}
