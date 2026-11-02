package com.org.pdfplatform.pdf;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.InvalidPdfException;
import com.org.pdfplatform.model.DocumentAnalysisModel;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

class PdfReaderModelTest {

    private final PdfReaderModel reader = new PdfReaderModel();

    @Test
    void readsTextPagesAndTagsPictureOnlyPages() throws Exception {
        DocumentAnalysisModel d = reader.read(PdfTestSupport.pdf("Hello gold world.", null, "Third page."), "a.pdf", 500, 2_000_000);

        assertEquals(3, d.totalPages());
        assertEquals(2, d.readablePages());
        assertTrue(d.pages().get(0).readable());
        assertTrue(d.pages().get(0).blocks().get(0).content().contains("Hello gold world."));
        assertFalse(d.pages().get(1).readable());
        assertEquals("IMAGE_ONLY", d.pages().get(1).blocks().get(0).type());
        assertEquals("", d.pages().get(1).blocks().get(0).content());      // never the literal "[Scanned]" again
        assertEquals(1, d.skippedContent().size());
        assertEquals("a.pdf", d.fileName());
    }

    @Test
    void readsEveryPageNotJustTheFirstTwenty() throws Exception {       // regression: old build stopped at page 20
        String[] texts = new String[25];
        for (int i = 0; i < 25; i++) texts[i] = "Page number " + (i + 1);

        DocumentAnalysisModel d = reader.read(PdfTestSupport.pdf(texts), "big.pdf", 500, 2_000_000);

        assertEquals(25, d.totalPages());
        assertEquals(25, d.pages().size());
        assertTrue(d.pages().get(24).blocks().get(0).content().contains("Page number 25"));
    }

    @Test
    void refusesTooManyPagesWithFriendlyError() throws Exception {
        InvalidPdfException e = assertThrows(InvalidPdfException.class, () -> reader.read(PdfTestSupport.pdf("a", "b", "c"), "x.pdf", 2, 2_000_000));
        assertEquals(ErrorCodeModel.TOO_MANY_PAGES, e.getCode());
        assertTrue(e.getMessage().contains("3 pages"));
    }

    @Test
    void garbageBytesAreReportedAsInvalidPdfNotAStackTrace() {
        InvalidPdfException e = assertThrows(InvalidPdfException.class,
                () -> reader.read("this is not a pdf".getBytes(), "x.pdf", 500, 2_000_000));
        assertEquals(ErrorCodeModel.INVALID_PDF, e.getCode());
    }

    @Test
    void documentIdIsUniquePerRead() throws Exception {
        byte[] bytes = PdfTestSupport.pdf("same");
        assertNotEquals(reader.read(bytes, "a", 500, 2_000_000).documentId(), reader.read(bytes, "a", 500, 2_000_000).documentId());
    }

    @Test
    void characterLimitMarksTheRestAsNotReadAndSaysSo() throws Exception {
        String big = "word ".repeat(40);                                   // ~200 chars per page
        DocumentAnalysisModel d = reader.read(PdfTestSupport.pdf(big, big, big, big), "b.pdf", 500, 450);

        assertTrue(d.truncated());
        assertEquals(2, d.readablePages());
        assertEquals("NOT_READ", d.pages().get(3).blocks().get(0).type());
        assertTrue(d.skippedContent().stream().anyMatch(m -> m.contains("character limit")));
    }

    @Test
    void englishTextIsDetectedAsEnglish() throws Exception {
        assertEquals("en", reader.read(PdfTestSupport.pdf("The quick brown fox jumps."), "e.pdf", 500, 2_000_000).detectedLanguage());
    }

    @Test
    void garbledFontDetectionNeedsMostlyPrivateUseGlyphs() {
        assertTrue(PdfReaderModel.looksGarbled("\uE001\uE002\uE003\uE004\uE005\uE006\uE007\uE008\uE009 ok"));
        assertTrue(PdfReaderModel.looksGarbled("\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD\uFFFD"));
        assertFalse(PdfReaderModel.looksGarbled("A normal English sentence with one \uFFFD glyph."));
        assertFalse(PdfReaderModel.looksGarbled("short"));
    }
}
