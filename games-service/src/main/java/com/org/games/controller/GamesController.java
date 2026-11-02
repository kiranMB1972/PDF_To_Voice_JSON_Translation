package com.org.games.controller;

import com.org.games.model.GameInfo;
import com.org.games.service.GameCatalogService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/games")
public class GamesController {

    private final GameCatalogService catalog;
    private final Executor executor;

    public GamesController(GameCatalogService catalog, @Qualifier("gamesExecutor") Executor executor) {
        this.catalog = catalog;
        this.executor = executor;
    }

    /** Same route and "character" parameter as the attached game-catalog-service. */
    @GetMapping
    public CompletableFuture<List<GameInfo>> list(@RequestParam(required = false) String character) {
        return CompletableFuture.supplyAsync(() -> catalog.forCharacter(character), executor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameInfo> one(@PathVariable String id) {
        return catalog.byId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
