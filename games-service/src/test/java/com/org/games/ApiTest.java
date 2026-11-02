package com.org.games;

import com.org.games.controller.AiController;
import com.org.games.controller.CharacterController;
import com.org.games.controller.GamesController;
import com.org.games.service.AiRecommendService;
import com.org.games.service.CharacterService;
import com.org.games.service.GameCatalogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiTest {

    private ExecutorService executor;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        mvc = MockMvcBuilders.standaloneSetup(
                new GamesController(new GameCatalogService(), executor),
                new CharacterController(new CharacterService(executor)),
                new AiController(new AiRecommendService(executor))).build();
    }

    @AfterEach
    void tearDown() { executor.close(); }

    private MvcResult run(MockHttpServletRequestBuilder b) throws Exception {
        MvcResult r = mvc.perform(b).andReturn();
        return r.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(r)).andReturn() : r;
    }

    @Test
    void gamesListAsyncAndFilteredByCharacter() throws Exception {
        MvcResult all = run(get("/api/games"));
        assertEquals(200, all.getResponse().getStatus());
        mvc.perform(asyncDispatch(mvc.perform(get("/api/games")).andReturn())).andExpect(jsonPath("$", hasSize(9)));
        mvc.perform(asyncDispatch(mvc.perform(get("/api/games").param("character", "charmaine")).andReturn()))
                .andExpect(jsonPath("$[?(@.id=='memory')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id=='maze')]", hasSize(0)));
    }

    @Test
    void oneGameByIdAndUnknownIs404() throws Exception {
        mvc.perform(get("/api/games/runner")).andExpect(status().isOk()).andExpect(jsonPath("$.file").value("hotstuff-fire-runner.html"));
        mvc.perform(get("/api/games/nope")).andExpect(status().isNotFound());
    }

    @Test
    void charactersAreServedAsynchronously() throws Exception {
        MvcResult r = run(get("/api/characters"));
        String body = r.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("Hotstuff") && body.contains("Princess Charmaine"), body);
        MvcResult one = run(get("/api/characters/charmaine"));
        org.junit.jupiter.api.Assertions.assertTrue(one.getResponse().getContentAsString().contains("Sparkle Magic"));
    }

    @Test
    void aiRecommendsByMood() throws Exception {
        assertEquals("maze", run(get("/api/ai/recommend").param("mood", "brave")).getResponse().getContentAsString().replaceAll(".*\"recommendedGameId\":\"([^\"]+)\".*", "$1"));
        assertEquals("highway-racer", run(get("/api/ai/recommend").param("mood", "I want speed")).getResponse().getContentAsString().replaceAll(".*\"recommendedGameId\":\"([^\"]+)\".*", "$1"));
        assertEquals("memory", run(get("/api/ai/recommend").param("mood", "???")).getResponse().getContentAsString().replaceAll(".*\"recommendedGameId\":\"([^\"]+)\".*", "$1"));
    }
}
