package com.org.pdfplatform.pdf;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.InvalidPdfException;
import com.org.pdfplatform.model.DocumentAnalysisModel;
import com.org.pdfplatform.model.PageModel;
import com.org.pdfplatform.model.TextBlockModel;
import com.org.pdfplatform.translate.ScriptDetector;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight PDF -> JSON: text only, page by page.
 *  - picture/scan pages are tagged IMAGE_ONLY and are never spoken (the old build spoke "[Scanned]"),
 *  - pages whose font cannot be decoded are tagged GARBLED_FONT instead of reading nonsense aloud,
 *  - nothing is invented: the old BharathaVani reader inserted made-up "Marusinchana ..." sentences into empty pages,
 *  - EVERY page is read (the old pdf-voice build stopped at page 20), up to the character limit - and that limit is reported.
 */
@Component
public class PdfReaderModel {

    public DocumentAnalysisModel read(byte[] pdfBytes, String fileName, int maxPages, int maxChars) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int total = doc.getNumberOfPages();
            if (total > maxPages) {
                throw new InvalidPdfException(ErrorCodeModel.TOO_MANY_PAGES,
                        "This PDF has " + total + " pages (limit " + maxPages + ")",
                        "Split the book into parts and read one part at a time.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<PageModel> pages = new ArrayList<>(total);
            int readable = 0, imageOnly = 0, garbled = 0, notRead = 0, chars = 0;
            StringBuilder sample = new StringBuilder();
            boolean truncated = false;
            for (int i = 1; i <= total; i++) {
                if (truncated) {
                    pages.add(new PageModel(i, false, List.of(TextBlockModel.notRead())));
                    notRead++;
                    continue;
                }
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc).strip();
                if (text.isEmpty()) {
                    pages.add(new PageModel(i, false, List.of(TextBlockModel.imageOnly())));
                    imageOnly++;
                } else if (looksGarbled(text)) {
                    pages.add(new PageModel(i, false, List.of(TextBlockModel.garbled())));
                    garbled++;
                } else if (chars + text.length() > maxChars) {
                    truncated = true;
                    pages.add(new PageModel(i, false, List.of(TextBlockModel.notRead())));
                    notRead++;
                } else {
                    chars += text.length();
                    readable++;
                    if (sample.length() < 4000) {
                        sample.append(text, 0, Math.min(text.length(), 1500)).append('\n');
                    }
                    pages.add(new PageModel(i, true, List.of(TextBlockModel.text(text))));
                }
            }
            List<String> skipped = new ArrayList<>();
            if (imageOnly > 0) {
                skipped.add(imageOnly + " of " + total + " page(s) are pictures or scans with no text and are skipped when reading aloud");
            }
            if (garbled > 0) {
                skipped.add(garbled + " page(s) use an old font whose text cannot be decoded (legacy Kannada Nudi/Baraha fonts) and are skipped - copying the text would give garbage");
            }
            if (notRead > 0) {
                skipped.add(notRead + " page(s) were not read because the " + maxChars + " character limit was reached");
            }
            return new DocumentAnalysisModel(UUID.randomUUID().toString(), fileName, total, readable,
                    ScriptDetector.detect(sample.toString()), truncated, chars, pages, skipped);
        } catch (InvalidPasswordException e) {
            throw new InvalidPdfException(ErrorCodeModel.PASSWORD_PROTECTED, "This PDF is password protected",
                    "Remove the password in a PDF tool, then upload it again.", e);
        } catch (IOException e) {
            throw new InvalidPdfException(ErrorCodeModel.INVALID_PDF, "This file could not be read as a PDF",
                    "Check that the file is a real, undamaged PDF.", e);
        }
    }

    /** More than a quarter of the visible characters are private-use glyphs or replacement characters. */
    static boolean looksGarbled(String text) {
        int visible = 0, bad = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            visible++;
            if (c == '\uFFFD' || (c >= '\uE000' && c <= '\uF8FF')) {
                bad++;
            }
        }
        return visible >= 8 && bad * 4 > visible;
    }
}
