package com.org.pdfplatform.controller;

import com.org.pdfplatform.exception.GlobalExceptionHandlerModel;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import com.org.pdfplatform.translate.OllamaClient;
import com.org.pdfplatform.translate.PromptBuilder;
import com.org.pdfplatform.translate.TranslationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TranslateAndAccentApiTest {

    private ExecutorService executor;
    private OllamaClient ollama;
    private MockMvc translate;
    private final MockMvc accents = MockMvcBuilders.standaloneSetup(new AccentApiController())
            .setControllerAdvice(new GlobalExceptionHandlerModel()).build();

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        ollama = mock(OllamaClient.class);
        when(ollama.getModel()).thenReturn("llama3.1");
        translate = build(5000);
    }

    @AfterEach
    void tearDown() { executor.close(); }

    private MockMvc build(long timeoutMs) {
        TranslationService svc = new TranslationService(ollama, new PromptBuilder(), 60000);
        return MockMvcBuilders.standaloneSetup(new TranslateApiController(svc, ollama, executor, timeoutMs))
                .setControllerAdvice(new GlobalExceptionHandlerModel()).build();
    }

    private MvcResult run(MockMvc mvc, MockHttpServletRequestBuilder b) throws Exception {
        MvcResult r = mvc.perform(b).andReturn();
        return r.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(r)).andReturn() : r;
    }

    private static MockHttpServletRequestBuilder req(String json) {
        return post("/api/v1/translate").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    @Test
    void translateReturnsTheTranslationAndTheEngineUsed() throws Exception {
        when(ollama.generate(anyString())).thenReturn("\u0CB6\u0CC1\u0CAD\u0CA6\u0CBF\u0CA8");

        MvcResult r = run(translate, req("{\"text\":\"Good morning\",\"target\":\"kn\",\"accent\":\"mysuru\",\"mode\":\"orate\"}"));

        assertEquals(200, r.getResponse().getStatus());
        String body = r.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"engine\":\"ollama:llama3.1\""), body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"target\":\"kn\"") && body.contains("\"sourceLanguage\":\"en\""), body);
    }

    @Test
    void engineOfflineIs503WithHowToStartItAndNoFakeText() throws Exception {
        when(ollama.generate(anyString())).thenThrow(new TranslationUnavailableException(
                "The translation engine (Ollama) is not running", "Start it: ollama serve", null));

        MvcResult r = run(translate, req("{\"text\":\"Hello\",\"target\":\"ta\"}"));

        assertEquals(503, r.getResponse().getStatus());
        String body = r.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("TRANSLATION_ENGINE_OFFLINE") && body.contains("ollama serve"));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("translation\""));
    }

    @Test
    void unknownLanguageAndBlankTextAre400() throws Exception {
        assertEquals(400, run(translate, req("{\"text\":\"Hello\",\"target\":\"zz\"}")).getResponse().getStatus());
        MvcResult blank = run(translate, req("{\"text\":\"   \",\"target\":\"kn\"}"));
        assertEquals(400, blank.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(blank.getResponse().getContentAsString().contains("EMPTY_TEXT"));
    }

    @Test
    void aTranslationThatNeverFinishesBecomesAClearTimeoutAnswer() throws Exception {
        when(ollama.generate(anyString())).thenAnswer(inv -> { Thread.sleep(3000); return "late"; });

        MvcResult r = run(build(200), req("{\"text\":\"Hello\",\"target\":\"te\"}"));

        assertEquals(503, r.getResponse().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(r.getResponse().getContentAsString().contains("took longer"));
    }

    @Test
    void statusSaysWhetherOllamaIsUp() throws Exception {
        when(ollama.isAvailable()).thenReturn(false);
        translate.perform(get("/api/v1/translate/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.hint").value(org.hamcrest.Matchers.containsString("ollama serve")));
        when(ollama.isAvailable()).thenReturn(true);
        translate.perform(get("/api/v1/translate/status")).andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void languagesEndpointListsTheEightLanguagesWithGroups() throws Exception {
        accents.perform(get("/api/languages")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].code").value("kn"))
                .andExpect(jsonPath("$[0].familyLabel").value("South Indian Languages"))
                .andExpect(jsonPath("$[0].voiceLocales[0]").value("kn-IN"))
                .andExpect(jsonPath("$[5].code").value("hi"))
                .andExpect(jsonPath("$[5].familyLabel").value("Other Indian Languages"))
                .andExpect(jsonPath("$[6].code").value("mr"))
                .andExpect(jsonPath("$[8].code").value("es"))
                .andExpect(jsonPath("$[8].familyLabel").value("International Languages"));
    }

    @Test
    void accentsComeAsDtosNotBareEnums() throws Exception {
        accents.perform(get("/api/accents").param("langCode", "kn")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("mysuru"))
                .andExpect(jsonPath("$[0].displayName").value("Mysuru"))
                .andExpect(jsonPath("$[0].voiceLocales[0]").value("kn-IN"));
        accents.perform(get("/api/accents").param("langCode", "es")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("standard"));
        accents.perform(get("/api/accents").param("langCode", "hi")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        accents.perform(get("/api/accents").param("langCode", "zz")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LANGUAGE"));
    }

    @Test
    void greetingFollowsTheHourOfTheDay() {
        assertEquals("Good morning", AccentApiController.greetingFor(7));
        assertEquals("Good afternoon", AccentApiController.greetingFor(13));
        assertEquals("Good evening", AccentApiController.greetingFor(18));
        assertEquals("Good night", AccentApiController.greetingFor(22));
    }
}
