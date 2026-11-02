package com.org.pdfplatform.translate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class TranslateConfig {

    private RestClient build(String baseUrl, int connectMs, int readMs) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Bean(name = "ollamaRest")
    public RestClient ollamaRest(@Value("${translate.ollama.base-url:http://localhost:11434}") String url,
                                 @Value("${translate.ollama.connect-timeout-ms:2000}") int connect,
                                 @Value("${translate.ollama.read-timeout-ms:170000}") int read) {
        return build(url, connect, read);
    }

    /** Short timeouts so the status chip on the page never hangs. */
    @Bean(name = "ollamaProbe")
    public RestClient ollamaProbe(@Value("${translate.ollama.base-url:http://localhost:11434}") String url) {
        return build(url, 1000, 2000);
    }

    /** Client for the free online translator. Follows the Windows/Java system proxy so it also works behind one. */
    @Bean(name = "webTranslateRest")
    public RestClient webTranslateRest(@Value("${translate.web.timeout-ms:25000}") int timeoutMs) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(5000))
                .proxy(ProxySelector.getDefault()).followRedirects(HttpClient.Redirect.NORMAL).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return RestClient.builder().requestFactory(factory).build();
    }
}
