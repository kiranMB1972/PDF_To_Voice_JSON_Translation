package com.org.games.controller;

import com.org.games.service.AiRecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiRecommendService svc;

    public AiController(AiRecommendService svc) {
        this.svc = svc;
    }

    @GetMapping("/recommend")
    public CompletableFuture<Map<String, Object>> recommend(@RequestParam(defaultValue = "fun") String mood) {
        return svc.recommend(mood);
    }
}
