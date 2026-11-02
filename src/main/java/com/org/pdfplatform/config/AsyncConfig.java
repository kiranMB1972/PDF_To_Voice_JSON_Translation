package com.org.pdfplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Virtual-thread executor used for PDF analysis so a big upload never blocks a web thread. */
@Configuration
public class AsyncConfig {

    @Bean(name = "pdfUploadExecutor", destroyMethod = "close")
    public ExecutorService pdfUploadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
