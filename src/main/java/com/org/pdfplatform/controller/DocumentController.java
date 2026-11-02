package com.org.pdfplatform.controller;

import com.org.pdfplatform.exception.UploadTimeoutExceptionModel;
import com.org.pdfplatform.model.DocumentAnalysisModel;
import com.org.pdfplatform.service.DocumentIntelligenceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentIntelligenceService service;
    private final Executor executor;
    private final long timeoutMs;

    public DocumentController(DocumentIntelligenceService service,
                              @Qualifier("pdfUploadExecutor") Executor executor,
                              @Value("${pdf-voice.upload-timeout-ms:300000}") long timeoutMs) {
        this.service = service;
        this.executor = executor;
        this.timeoutMs = timeoutMs;
    }

    /** The analysis runs on a virtual thread under a hard time limit (the earlier build had no limit at all). */
    @PostMapping("/analyze-sync")
    public ResponseEntity<DocumentAnalysisModel> analyzeSync(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        String name = file.getOriginalFilename();
        CompletableFuture<DocumentAnalysisModel> job = CompletableFuture
                .supplyAsync(() -> service.analyze(bytes, name), executor)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        try {
            return ResponseEntity.ok(job.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UploadTimeoutExceptionModel(timeoutMs, String.valueOf(name));
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof CompletionException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof TimeoutException) {
                throw new UploadTimeoutExceptionModel(timeoutMs, String.valueOf(name));
            }
            if (cause instanceof RuntimeException re) {
                throw re;                                   // InvalidPdfException etc. -> the advice maps it
            }
            throw new IllegalStateException(cause);
        }
    }
}
