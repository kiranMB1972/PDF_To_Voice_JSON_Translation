package com.org.games.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * "AI recommendation": matches the player's mood to a game. (The attached version reassigned a lambda
 * parameter, which does not compile; this one is a pure function.)
 */
@Service
public class AiRecommendService {

    private final Executor executor;

    public AiRecommendService(@Qualifier("gamesExecutor") Executor executor) {
        this.executor = executor;
    }

    public CompletableFuture<Map<String, Object>> recommend(String mood) {
        return CompletableFuture.supplyAsync(() -> pick(mood), executor);
    }

    static Map<String, Object> pick(String rawMood) {
        String mood = rawMood == null || rawMood.isBlank() ? "fun" : rawMood.trim().toLowerCase(Locale.ROOT);
        String game;
        String why;
        if (mood.contains("brave")) { game = "maze"; why = "Brave? Hotstuff's Fire Maze is a bold adventure."; }
        else if (mood.contains("puzzle") || mood.contains("think")) { game = "memory"; why = "Puzzle mood: Charmaine's Memory Flip trains the memory."; }
        else if (mood.contains("magic")) { game = "stars"; why = "Magic mood: catch Charmaine's falling stars."; }
        else if (mood.contains("smart") || mood.contains("quiz")) { game = "quiz"; why = "Feeling smart? Try the quiz."; }
        else if (mood.contains("race") || mood.contains("car") || mood.contains("speed")) { game = "highway-racer"; why = "Need for speed: Gold Highway Racer."; }
        else if (mood.contains("bike")) { game = "moto-dash"; why = "Bike mood: Moto Dash."; }
        else if (mood.contains("fun") || mood.contains("run")) { game = "runner"; why = "Fun mood: run and jump with Hotstuff."; }
        else { game = "memory"; why = "A gentle game to start with."; }
        return Map.of("mood", mood, "inputMood", mood, "recommendedGameId", game, "reason", why,
                "characters", List.of("hotstuff", "charmaine"));
    }
}
