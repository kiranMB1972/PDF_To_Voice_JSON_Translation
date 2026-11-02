package com.org.pdfplatform.translate;

import com.org.pdfplatform.model.AppLanguage;
import com.org.pdfplatform.model.RegionalAccent;
import com.org.pdfplatform.translate.PromptBuilder.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private final PromptBuilder pb = new PromptBuilder();

    @Test
    void promptCarriesTheTextTheTargetAndTheNoInventionRule() {
        String p = pb.build("Good morning class.", AppLanguage.KANNADA, RegionalAccent.MYSURU, Mode.TRANSLATE, "English");
        assertTrue(p.contains("<text>\nGood morning class.\n</text>"));
        assertTrue(p.contains("from English into Kannada"));
        assertTrue(p.contains("Do not add, remove or invent"));
        assertTrue(p.contains("Mysuru"));
    }

    @Test
    void promptIsNotTiedToOneTextbook() {                       // the old prompts hard-coded "10th Standard Marusinchana"
        String p = pb.build("Hello", AppLanguage.TAMIL, RegionalAccent.STANDARD, Mode.TRANSLATE, "English");
        assertFalse(p.toLowerCase().contains("marusinchana"));
        assertFalse(p.contains("10th"));
        assertFalse(p.contains("Prefer wording natural to"));   // STANDARD adds no accent line
    }

    @Test
    void modesChangeTheInstruction() {
        assertTrue(pb.build("x", AppLanguage.TELUGU, null, Mode.TEACH, "English").contains("Words to learn"));
        assertTrue(pb.build("x", AppLanguage.TELUGU, null, Mode.ORATE, "English").contains("read aloud"));
        assertTrue(pb.build("x", AppLanguage.TELUGU, null, Mode.TRANSLATE, "English").contains("translation only"));
    }

    @Test
    void modeParsingIsForgiving() {
        assertEquals(Mode.TEACH, Mode.of(" teach "));
        assertEquals(Mode.TRANSLATE, Mode.of(null));
        assertEquals(Mode.TRANSLATE, Mode.of("nonsense"));
    }

    @Test
    void cleanRemovesWrappersSmallModelsAdd() {
        assertEquals("\u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0", pb.clean("```\n\u0CA8\u0CAE\u0CB8\u0CCD\u0C95\u0CBE\u0CB0\n```"));
        assertEquals("Bonjour", pb.clean("<text>\nBonjour\n</text>"));
        assertEquals("Hola", pb.clean("Here is the translation:\n\nHola"));
        assertEquals("", pb.clean(null));
    }
}
