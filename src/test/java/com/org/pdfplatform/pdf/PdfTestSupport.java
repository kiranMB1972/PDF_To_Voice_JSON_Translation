package com.org.pdfplatform.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;

/** Test helper: builds real PDFs in memory. */
public final class PdfTestSupport {

    private PdfTestSupport() { }

    /** Builds a PDF in memory: a page per entry; null entry = a blank (picture-only) page. */
    public static byte[] pdf(String... pageTexts) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String t : pageTexts) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (t != null) {
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        cs.newLineAtOffset(72, 700);
                        cs.showText(t);
                        cs.endText();
                    }
                }
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
