package com.org.pdfplatform.service;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.InvalidPdfException;
import com.org.pdfplatform.pdf.PdfReaderModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentIntelligenceServiceTest {

    private final DocumentIntelligenceService svc = new DocumentIntelligenceService(new PdfReaderModel(), 500, 2_000_000);

    @Test
    void emptyUploadIsRejectedClearly() {
        InvalidPdfException e = assertThrows(InvalidPdfException.class, () -> svc.analyze(new byte[0], "a.pdf"));
        assertEquals(ErrorCodeModel.EMPTY_FILE, e.getCode());
        assertThrows(InvalidPdfException.class, () -> svc.analyze(null, "a.pdf"));
    }

    @Test
    void missingFileNameGetsADefault() throws Exception {
        var d = svc.analyze(com.org.pdfplatform.pdf.PdfTestSupport.pdf("hi"), " ");
        assertEquals("document.pdf", d.fileName());
    }
}
