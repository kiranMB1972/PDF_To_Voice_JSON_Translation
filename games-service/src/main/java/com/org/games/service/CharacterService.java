package com.org.games.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** The two characters, served asynchronously (CompletableFuture on virtual threads) like the attached project. */
@Service
public class CharacterService {

    private final Executor executor;

    public CharacterService(@Qualifier("gamesExecutor") Executor executor) {
        this.executor = executor;
    }

    public CompletableFuture<Map<String, Object>> hotstuff() {
        return CompletableFuture.supplyAsync(() -> Map.<String, Object>of(
                "id", "hotstuff", "name", "Hotstuff", "type", "Little Devil",
                "power", "Fire Tricks, Kind Heart", "games", List.of("Fire Maze", "Fire Runner", "Quiz"), "avatar", "\uD83D\uDD25"), executor);
    }

    public CompletableFuture<Map<String, Object>> charmaine() {
        return CompletableFuture.supplyAsync(() -> Map.<String, Object>of(
                "id", "charmaine", "name", "Princess Charmaine", "type", "Forest Princess",
                "power", "Sparkle Magic, Puzzle Master", "games", List.of("Memory Flip", "Star Catcher", "Quiz"), "avatar", "\uD83D\uDC51"), executor);
    }

    public CompletableFuture<List<Map<String, Object>>> all() {
        CompletableFuture<Map<String, Object>> h = hotstuff();
        CompletableFuture<Map<String, Object>> c = charmaine();
        return h.thenCombine(c, (a, b) -> List.of(a, b));
    }
}
