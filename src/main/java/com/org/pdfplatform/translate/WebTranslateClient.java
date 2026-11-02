package com.org.pdfplatform.translate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.pdfplatform.exception.TranslationUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Keyless online translation with two free endpoints, tried in this order:
 * 1) the public Google Translate web endpoint (no key, no account, handles all ten languages of this app),
 * 2) MyMemory (free, small daily limit) as a second chance.
 * Both are public best-effort services: they can rate-limit or change without notice. For a production setup
 * use Ollama or a paid engine. Switch this client off with translate.web.enabled=false to keep every page local.
 */
@Component
public class WebTranslateClient implements WebTranslator {

    private static final Logger log = LoggerFactory.getLogger(WebTranslateClient.class);
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36 Edg/124.0";
    private static final Pattern CODE = Pattern.compile("[A-Za-z]{2,3}(-[A-Za-z]{2,4})?");
    private static final int MYMEMORY_MAX = 450;                      // MyMemory accepts at most 500 bytes per request

    private final RestClient rest;
    private final boolean enabled;
    private final String googleUrl;
    private final String myMemoryUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public WebTranslateClient(@Qualifier("webTranslateRest") RestClient rest,
                              @Value("${translate.web.enabled:true}") boolean enabled,
                              @Value("${translate.web.google-url:https://translate.googleapis.com/translate_a/single}") String googleUrl,
                              @Value("${translate.web.mymemory-url:https://api.mymemory.translated.net/get}") String myMemoryUrl) {
        this.rest = rest;
        this.enabled = enabled;
        this.googleUrl = googleUrl;
        this.myMemoryUrl = myMemoryUrl;
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public String name() { return "online-translator"; }

    @Override
    public String translate(String text, String sourceCode, String targetCode) {
        String src = (sourceCode == null || sourceCode.isBlank()) ? "auto" : sourceCode.trim();
        String tgt = targetCode == null ? "" : targetCode.trim();
        if (!CODE.matcher(tgt).matches() || !(src.equals("auto") || CODE.matcher(src).matches())) {
            throw new TranslationUnavailableException("Unsupported language code for the online translator",
                    "Choose a language from the list.", null);
        }
        List<String> why = new ArrayList<>();
        RuntimeException last;
        try {
            return viaGoogle(text, src, tgt);
        } catch (RuntimeException e) {
            last = e;
            why.add("Google POST: " + brief(e));
        }
        try {
            return viaGoogleGet(text, src, tgt);
        } catch (RuntimeException e) {
            last = e;
            why.add("Google GET: " + brief(e));
        }
        try {
            return viaMyMemory(text, src, tgt);
        } catch (RuntimeException e) {
            last = e;
            why.add("MyMemory: " + brief(e));
        }
        log.warn("Online translation failed: {}", why);
        throw new TranslationUnavailableException("The online translator could not be reached",
                "Check that this computer is connected to the internet and that a firewall/proxy is not blocking "
                        + "translate.googleapis.com, then press the engine chip to retry (or start Ollama: ollama serve). Details: "
                        + String.join(" | ", why), last);
    }

    @Override
    public List<String> diagnose(String sample, String target) {
        List<String> out = new ArrayList<>();
        String[] names = {"Google POST", "Google GET", "MyMemory"};
        for (int i = 0; i < names.length; i++) {
            try {
                String r = i == 0 ? viaGoogle(sample, "auto", target) : i == 1 ? viaGoogleGet(sample, "auto", target) : viaMyMemory(sample, "en", target);
                out.add(names[i] + ": OK -> " + (r.length() > 60 ? r.substring(0, 60) + "..." : r));
            } catch (RuntimeException e) {
                out.add(names[i] + ": FAILED - " + brief(e));
            }
        }
        return out;
    }

    static String brief(RuntimeException e) {
        if (e instanceof RestClientResponseException r) {
            return "HTTP " + r.getStatusCode().value();
        }
        String m = e.getMessage() == null ? "" : e.getMessage().replaceAll("\\s+", " ");
        Throwable c = e.getCause();
        if (c != null && c.getMessage() != null && m.length() < 40) {
            m = m + " (" + c.getMessage().replaceAll("\\s+", " ") + ")";
        }
        return e.getClass().getSimpleName() + (m.isEmpty() ? "" : ": " + (m.length() > 140 ? m.substring(0, 140) : m));
    }

    private String viaGoogleGet(String text, String src, String tgt) {
        StringBuilder out = new StringBuilder();
        for (String part : TranslationService.chunk(text, 300)) {
            String q = URLEncoder.encode(part, StandardCharsets.UTF_8);
            URI uri = URI.create(googleUrl + "?client=gtx&sl=" + src + "&tl=" + tgt + "&dt=t&q=" + q);
            byte[] body = rest.get().uri(uri).header(HttpHeaders.USER_AGENT, UA).retrieve().body(byte[].class);
            if (out.length() > 0) {
                out.append("\n");
            }
            out.append(parseGoogle(readJson(body)));
        }
        return out.toString();
    }

    private String viaGoogle(String text, String src, String tgt) {
        URI uri = URI.create(googleUrl + "?client=gtx&sl=" + src + "&tl=" + tgt + "&dt=t");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("q", text);
        byte[] body = rest.post().uri(uri).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.USER_AGENT, UA).body(form).retrieve().body(byte[].class);
        return parseGoogle(readJson(body));
    }

    private String viaMyMemory(String text, String src, String tgt) {
        String from = "auto".equals(src) ? "en" : src;                 // MyMemory has no auto-detect
        StringBuilder out = new StringBuilder();
        for (String part : TranslationService.chunk(text, MYMEMORY_MAX)) {
            String q = URLEncoder.encode(part, StandardCharsets.UTF_8);
            URI uri = URI.create(myMemoryUrl + "?q=" + q + "&langpair=" + from + "%7C" + tgt);
            byte[] body = rest.get().uri(uri).header(HttpHeaders.USER_AGENT, UA).retrieve().body(byte[].class);
            if (out.length() > 0) {
                out.append("\n\n");
            }
            out.append(parseMyMemory(readJson(body)));
        }
        return out.toString();
    }

    private JsonNode readJson(byte[] body) {
        if (body == null || body.length == 0) {
            throw new IllegalStateException("empty answer");
        }
        try {
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new IllegalStateException("answer is not JSON", e);
        }
    }

    /** Google answer: [[["translated","original",...],["next","next original",...]], null, "en", ...] */
    static String parseGoogle(JsonNode root) {
        JsonNode segments = root == null ? null : root.path(0);
        if (segments == null || !segments.isArray() || segments.isEmpty()) {
            throw new IllegalStateException("unexpected answer");
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode seg : segments) {
            sb.append(seg.path(0).asText(""));
        }
        String s = sb.toString().strip();
        if (s.isEmpty()) {
            throw new IllegalStateException("empty translation");
        }
        return s;
    }

    /** MyMemory answer: {"responseData":{"translatedText":"..."},"responseStatus":200,...} */
    static String parseMyMemory(JsonNode root) {
        if (root == null) {
            throw new IllegalStateException("empty answer");
        }
        int status = root.path("responseStatus").asInt(200);
        String t = root.path("responseData").path("translatedText").asText("");
        if (status != 200 || t.isBlank() || t.toUpperCase().contains("MYMEMORY WARNING")) {
            throw new IllegalStateException("MyMemory refused (status " + status + ")");
        }
        return t.strip();
    }
}
