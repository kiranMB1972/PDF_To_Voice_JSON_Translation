package com.org.games.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Java 21 virtual threads for every CompletableFuture stage. */
@Configuration
public class AsyncConfig {

    @Bean(name = "gamesExecutor", destroyMethod = "close")
    public ExecutorService gamesExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
