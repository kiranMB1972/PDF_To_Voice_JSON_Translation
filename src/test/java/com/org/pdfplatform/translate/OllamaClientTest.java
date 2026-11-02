package com.org.pdfplatform.translate;

import com.org.pdfplatform.exception.TranslationUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OllamaClientTest {

    private static final String BASE = "http://ollama.test";
    private MockRestServiceServer server;
    private MockRestServiceServer probeServer;
    private OllamaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder main = RestClient.builder().baseUrl(BASE);
        RestClient.Builder probe = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(main).build();
        probeServer = MockRestServiceServer.bindTo(probe).build();
        client = new OllamaClient(main.build(), probe.build(), "llama3.1", BASE);
    }

    @Test
    void generateSendsModelPromptAndReadsTheResponseField() {
        server.expect(requestTo(BASE + "/api/generate")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("llama3.1"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.prompt").value("translate this"))
                .andRespond(withSuccess("{\"response\":\"done\",\"done\":true}", MediaType.APPLICATION_JSON));

        assertEquals("done", client.generate("translate this"));
        server.verify();
    }

    @Test
    void ollamaNotRunningIsReportedWithHowToStartIt() {
        server.expect(requestTo(BASE + "/api/generate")).andRespond(withException(new ConnectException("refused")));

        TranslationUnavailableException e = assertThrows(TranslationUnavailableException.class, () -> client.generate("x"));
        assertTrue(e.getMessage().contains("not running"));
        assertTrue(e.getGuidance().contains("ollama serve"));
    }

    @Test
    void missingModelTellsTheUserToPullIt() {
        server.expect(requestTo(BASE + "/api/generate")).andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND)
                .body("{\"error\":\"model 'llama3.1' not found\"}").contentType(MediaType.APPLICATION_JSON));

        TranslationUnavailableException e = assertThrows(TranslationUnavailableException.class, () -> client.generate("x"));
        assertTrue(e.getGuidance().contains("ollama pull llama3.1"));
    }

    @Test
    void emptyAnswerIsNotTreatedAsATranslation() {
        server.expect(requestTo(BASE + "/api/generate")).andRespond(withSuccess("{\"response\":\"  \"}", MediaType.APPLICATION_JSON));
        assertThrows(TranslationUnavailableException.class, () -> client.generate("x"));
    }

    @Test
    void serverErrorBecomesUnavailable() {
        server.expect(requestTo(BASE + "/api/generate")).andRespond(withServerError());
        assertThrows(TranslationUnavailableException.class, () -> client.generate("x"));
    }

    @Test
    void probeReportsUpAndDownWithoutThrowing() {
        probeServer.expect(requestTo(BASE + "/api/tags")).andRespond(withSuccess("{\"models\":[]}", MediaType.APPLICATION_JSON));
        assertTrue(client.isAvailable());
        probeServer.reset();
        probeServer.expect(requestTo(BASE + "/api/tags")).andRespond(withException(new ConnectException("refused")));
        assertFalse(client.isAvailable());
    }
}
