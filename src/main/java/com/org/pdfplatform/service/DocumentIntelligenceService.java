package com.org.pdfplatform.service;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.InvalidPdfException;
import com.org.pdfplatform.model.DocumentAnalysisModel;
import com.org.pdfplatform.pdf.PdfReaderModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DocumentIntelligenceService {

    private final PdfReaderModel reader;
    private final int maxPages;
    private final int maxChars;

    public DocumentIntelligenceService(PdfReaderModel reader, @Value("${pdf-voice.max-pages:1000}") int maxPages,
                                       @Value("${pdf-voice.max-chars:2000000}") int maxChars) {
        this.reader = reader;
        this.maxPages = maxPages;
        this.maxChars = maxChars;
    }

    public DocumentAnalysisModel analyze(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new InvalidPdfException(ErrorCodeModel.EMPTY_FILE, "The file is empty", "Choose a PDF that has content.");
        }
        return reader.read(bytes, fileName == null || fileName.isBlank() ? "document.pdf" : fileName, maxPages, maxChars);
    }
}
