package com.org.pdfplatform.controller;

import com.org.pdfplatform.exception.ErrorCodeModel;
import com.org.pdfplatform.exception.GlobalExceptionHandlerModel;
import com.org.pdfplatform.exception.InvalidPdfException;
import com.org.pdfplatform.model.DocumentAnalysisModel;
import com.org.pdfplatform.model.PageModel;
import com.org.pdfplatform.model.TextBlockModel;
import com.org.pdfplatform.pdf.PdfReaderModel;
import com.org.pdfplatform.pdf.PdfTestSupport;
import com.org.pdfplatform.service.DocumentIntelligenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private ExecutorService executor;
    private DocumentIntelligenceService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        service = mock(DocumentIntelligenceService.class);
        mvc = build(300000);
    }

    @AfterEach
    void tearDown() { executor.close(); }

    private MockMvc build(long timeoutMs) {
        return MockMvcBuilders.standaloneSetup(new DocumentController(service, executor, timeoutMs))
                .setControllerAdvice(new GlobalExceptionHandlerModel()).build();
    }

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "a.pdf", "application/pdf", "%PDF".getBytes());
    }

    @Test
    void analyzeReturnsThePagesAsJson() throws Exception {
        when(service.analyze(any(), any())).thenReturn(new DocumentAnalysisModel("id1", "a.pdf", 1, 1, "en", false, 5,
                List.of(new PageModel(1, true, List.of(TextBlockModel.text("hello")))), List.of()));

        mvc.perform(multipart("/api/v1/documents/analyze-sync").file(file()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pages[0].blocks[0].content").value("hello"));
    }

    @Test
    void badPdfBecomesA422WithGuidanceNotAStackTrace() throws Exception {
        when(service.analyze(any(), any())).thenThrow(new InvalidPdfException(ErrorCodeModel.INVALID_PDF, "bad", "fix it"));

        mvc.perform(multipart("/api/v1/documents/analyze-sync").file(file()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PDF"))
                .andExpect(jsonPath("$.userGuidance").value("fix it"));
    }

    @Test
    void anAnalysisThatNeverFinishesIsCutOffWith408() throws Exception {      // the old build had no time limit at all
        when(service.analyze(any(), any())).thenAnswer(inv -> { Thread.sleep(3000); return null; });

        build(200).perform(multipart("/api/v1/documents/analyze-sync").file(file()))
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("$.code").value("TIMEOUT"));
    }

    @Test
    void unexpectedFailuresNeverLeakInternals() throws Exception {
        when(service.analyze(any(), any())).thenThrow(new IllegalStateException("secret internal detail"));

        mvc.perform(multipart("/api/v1/documents/analyze-sync").file(file()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error").value("Something went wrong while processing the PDF"));
    }

    @Test
    void endToEndWithTheRealReaderOnARealGeneratedPdf() throws Exception {
        var real = new DocumentIntelligenceService(new PdfReaderModel(), 500, 2_000_000);
        MockMvc m = MockMvcBuilders.standaloneSetup(new DocumentController(real, executor, 60000))
                .setControllerAdvice(new GlobalExceptionHandlerModel()).build();

        m.perform(multipart("/api/v1/documents/analyze-sync")
                        .file(new MockMultipartFile("file", "r.pdf", "application/pdf", PdfTestSupport.pdf("Gold text", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.readablePages").value(1))
                .andExpect(jsonPath("$.pages[1].blocks[0].type").value("IMAGE_ONLY"));
    }
}
