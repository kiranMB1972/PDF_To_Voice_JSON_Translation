package com.org.pdfplatform.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Tells the page where the separate Games microservice lives (the "Entertainment Games" drop-down loads from it). */
@RestController
@RequestMapping("/api")
public class AppConfigController {

    private final String gamesServiceUrl;

    public AppConfigController(@Value("${games.service.url:http://localhost:8095}") String gamesServiceUrl) {
        this.gamesServiceUrl = gamesServiceUrl.endsWith("/") ? gamesServiceUrl.substring(0, gamesServiceUrl.length() - 1) : gamesServiceUrl;
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("gamesServiceUrl", gamesServiceUrl);
    }
}
