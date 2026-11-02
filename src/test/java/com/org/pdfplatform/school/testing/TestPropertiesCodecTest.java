package com.org.pdfplatform.school.testing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPropertiesCodecTest {

    private static TestSpec sample() {
        QuestionSpec q1 = new QuestionSpec("What is 2+2?\nChoose all that apply: =, :  \\ test", List.of(" four", "4", "five", "\u0C95\u0CA8\u0CCD\u0CA8\u0CA1"), new LinkedHashSet<>(List.of("A", "B")), 2);
        QuestionSpec q2 = new QuestionSpec("Capital of India?", List.of("Delhi", "Mumbai"), new LinkedHashSet<>(List.of("A")), 1);
        return new TestSpec("Unit Test #1: Maths", 7, "Maths", LocalDate.of(2026, 10, 29), 30, 12L, List.of(5L, 6L), List.of(q1, q2));
    }

    @Test
    void aTestSurvivesTheRoundTripThroughThePropertiesFile() {
        TestSpec t = sample();
        assertEquals(t, TestPropertiesCodec.read(TestPropertiesCodec.write(t)));
    }

    @Test
    void theFileHasReadableKeysInOrderAndKeepsUnicodeAsIs() {
        String f = TestPropertiesCodec.write(sample());
        assertTrue(f.contains("test.date=29-10-2026"));
        assertTrue(f.contains("test.class=7"));
        assertTrue(f.contains("question.count=2"));
        assertTrue(f.contains("question.1.option.D=\u0C95\u0CA8\u0CCD\u0CA8\u0CA1"));
        assertTrue(f.indexOf("question.1.text") < f.indexOf("question.2.text"));
        assertTrue(f.contains("question.1.correct=A,B"));
    }

    @Test
    void specialCharactersAreEscapedSoTheyCannotBreakTheFile() {
        assertEquals("a\\\\b\\nc", TestPropertiesCodec.escape("a\\b\nc"));
        assertEquals("\\ lead", TestPropertiesCodec.escape(" lead"));
    }

    @Test
    void aFileWithoutADateOrWithABadDateIsRejectedWithAClearMessage() {
        assertThrows(IllegalArgumentException.class, () -> TestPropertiesCodec.read("test.title=x\nquestion.count=0\n"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TestPropertiesCodec.read("test.date=2026-10-29\n"));
        assertTrue(e.getMessage().contains("29-10-2026"));
    }

    @Test
    void fileNamesFollowTestDdMmYyyyAndTheSecondTestOfADayGetsASuffix() {
        LocalDate d = LocalDate.of(2026, 10, 29);
        assertEquals("Test29-10-2026.properties", TestFileNames.forDate(d));
        assertEquals("Test29-10-2026.properties", TestFileNames.forDate(d, 1));
        assertEquals("Test29-10-2026_2.properties", TestFileNames.forDate(d, 2));
        assertEquals("Test01-01-2027.properties", TestFileNames.forDate(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void gradingGivesMarksOnlyForAnExactMatchOfTheTickedBoxes() {
        TestSpec t = TestPropertiesCodec.read(TestPropertiesCodec.write(sample()));
        Grader.Result r = Grader.grade(t, Map.of(1, new LinkedHashSet<>(List.of("B", "A")), 2, new TreeSet<>(List.of("B"))));
        assertEquals(2, r.score());
        assertEquals(3, r.total());
        assertTrue(r.correctPerQuestion()[0]);
        assertFalse(r.correctPerQuestion()[1]);
        assertEquals(0, Grader.grade(t, Map.of()).score());
        Set<String> onlyOne = new TreeSet<>(List.of("A"));
        assertEquals(0, Grader.grade(t, Map.of(1, onlyOne)).score());                    // a missing correct box is not enough
    }

    @Test
    void theJsonTwinIsValidAndStudentSafeWhenAnswersAreLeftOut() {
        String withAnswers = TestJsonExporter.toJson(sample(), "Test29-10-2026.json", true);
        String safe = TestJsonExporter.toJson(sample(), "Test29-10-2026.json", false);
        assertTrue(withAnswers.contains("\"correct\": [\"A\", \"B\"]"));
        assertFalse(safe.contains("correct"));
        assertTrue(withAnswers.contains("\"dateDMY\": \"29-10-2026\""));
        assertTrue(withAnswers.contains("\"totalMarks\": 3"));
        assertTrue(withAnswers.contains("What is 2+2?\\nChoose all that apply"));
        assertEquals("\"say \\\"hi\\\"\\n\"", TestJsonExporter.str("say \"hi\"\n"));
    }
}
