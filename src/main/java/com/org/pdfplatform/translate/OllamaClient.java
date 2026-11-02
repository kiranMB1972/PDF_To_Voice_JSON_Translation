package com.org.pdfplatform.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Plain REST client for a local Ollama server (POST /api/generate). Replaces the Spring AI 1.0.0-M6 milestone
 * starter of the earlier build: same Ollama, no milestone repository, and every failure is reported honestly.
 */
@Component
public class OllamaClient {

    private final RestClient rest;
    private final RestClient probe;
    private final String model;
    private final String baseUrl;

    public OllamaClient(@Qualifier("ollamaRest") RestClient rest, @Qualifier("ollamaProbe") RestClient probe,
                        @Value("${translate.ollama.model:llama3.1}") String model,
                        @Value("${translate.ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.rest = rest;
        this.probe = probe;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    public String getModel() { return model; }

    public String getBaseUrl() { return baseUrl; }

    /** True when the server answers; false otherwise (never throws). */
    public boolean isAvailable() {
        try {
            probe.get().uri("/api/tags").retrieve().toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String generate(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("stream", false);
        body.put("options", Map.of("temperature", 0.2));
        try {
            JsonNode json = rest.post().uri("/api/generate").contentType(MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(JsonNode.class);
            String text = json == null ? null : json.path("response").asText(null);
            if (text == null || text.isBlank()) {
                throw new TranslationUnavailableException("The translation engine returned an empty answer",
                        "Try again, or use a larger model (ollama pull " + model + ").", null);
            }
            return text;
        } catch (TranslationUnavailableException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 404) {
                throw new TranslationUnavailableException("Model '" + model + "' is not installed in Ollama",
                        "Run:  ollama pull " + model, e);
            }
            throw new TranslationUnavailableException("The translation engine answered HTTP " + e.getStatusCode().value(),
                    "Check the Ollama window for errors.", e);
        } catch (ResourceAccessException e) {
            throw new TranslationUnavailableException("The translation engine (Ollama) is not running",
                    "Start it:  ollama serve   then:  ollama pull " + model + "   (free, runs on this computer).", e);
        } catch (RestClientException e) {
            throw new TranslationUnavailableException("The translation engine could not be used: " + e.getMessage(),
                    "Check that Ollama is running at " + baseUrl, e);
        }
    }
}
