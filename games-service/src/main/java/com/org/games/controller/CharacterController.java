package com.org.games.controller;

import com.org.games.service.CharacterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/characters")
public class CharacterController {

    private final CharacterService svc;

    public CharacterController(CharacterService svc) {
        this.svc = svc;
    }

    @GetMapping
    public CompletableFuture<List<Map<String, Object>>> all() { return svc.all(); }

    @GetMapping("/hotstuff")
    public CompletableFuture<Map<String, Object>> hotstuff() { return svc.hotstuff(); }

    @GetMapping("/charmaine")
    public CompletableFuture<Map<String, Object>> charmaine() { return svc.charmaine(); }
}
