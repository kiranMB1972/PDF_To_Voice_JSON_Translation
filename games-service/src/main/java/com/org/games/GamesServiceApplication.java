package com.org.games;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entertainment Games microservice: its own folder, its own port (8095), used by the main app's top-right drop-down. */
@SpringBootApplication
public class GamesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GamesServiceApplication.class, args);
    }
}
