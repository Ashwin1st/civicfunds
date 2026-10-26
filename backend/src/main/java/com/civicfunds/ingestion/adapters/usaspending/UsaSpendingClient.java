package com.civicfunds.ingestion.adapters.usaspending;

import com.civicfunds.ingestion.IngestionException;
import com.civicfunds.ingestion.SourceSystem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin HTTP client for the USAspending v2 API, with retry and timeouts.
 *
 * <p>Retries only on transport failures and 5xx/429. A 4xx is a request the
 * server understood and rejected; retrying it just wastes time and hides a bug.
 */
@Component
public class UsaSpendingClient {

    private static final Logger log = LoggerFactory.getLogger(UsaSpendingClient.class);

    private final RestClient restClient;
    private final UsaSpendingProperties properties;
    private final ObjectMapper objectMapper;

    public UsaSpendingClient(UsaSpendingProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "CivicFunds/0.1 (Congressional App Challenge project)")
                .build();
    }

    /** POSTs a JSON body and returns the parsed response, retrying transient failures. */
    public JsonNode post(String path, Object body) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= properties.maxRetries() + 1; attempt++) {
            try {
                String raw = restClient.post()
                        .uri(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                if (raw == null || raw.isBlank()) {
                    throw new IngestionException(SourceSystem.USASPENDING,
                            "Empty response from " + path);
                }
                return objectMapper.readTree(raw);

            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // 4xx: our request is wrong. Do not retry - surface it.
                throw new IngestionException(SourceSystem.USASPENDING,
                        "USAspending rejected the request to " + path + ": "
                        + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);

            } catch (Exception e) {
                last = new IngestionException(SourceSystem.USASPENDING,
                        "Call to " + path + " failed on attempt " + attempt, e);
                log.warn("USAspending {} attempt {}/{} failed: {}",
                        path, attempt, properties.maxRetries() + 1, e.toString());
                sleepBeforeRetry(attempt);
            }
        }
        throw last;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(properties.retryBackoff().toMillis() * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IngestionException(SourceSystem.USASPENDING, "Ingestion interrupted", ie);
        }
    }
}
