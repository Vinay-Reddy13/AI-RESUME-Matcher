package com.example.matcher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.HashMap;

/**
 * Service for communicating with the NLP service
 */
@Service
public class NlpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NlpClient.class);
    
    private final WebClient nlpWebClient;
    
    @Value("${TOP_K:5}")
    private int defaultTopK;
    
    @Autowired
    public NlpClient(@Qualifier("nlpWebClient") WebClient nlpWebClient) {
        this.nlpWebClient = nlpWebClient;
    }
    
    /**
     * Check if NLP service is healthy
     */
    public Mono<Boolean> isHealthy() {
        return nlpWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> "ok".equals(response.get("status")))
                .onErrorReturn(false)
                .doOnSuccess(healthy -> logger.info("NLP service health check: {}", healthy));
    }
    
    /**
     * Build the search index
     */
    public Mono<Map<String, Object>> buildIndex() {
        logger.info("Building NLP search index...");
        
        return nlpWebClient.post()
                .uri("/index/build")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(response -> logger.info("Index build response: {}", response))
                .doOnError(error -> logger.error("Error building index: {}", error.getMessage()));
    }
    
    /**
     * Search for matching jobs with role-based filtering
     */
    public Mono<Map<String, Object>> search(String query, Integer topK, String role) {
        int searchTopK = topK != null ? topK : defaultTopK;
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", query);
        requestBody.put("top_k", searchTopK);
        if (role != null && !role.trim().isEmpty()) {
            requestBody.put("role", role);
        }
        
        logger.info("Searching NLP service for query: {} (topK: {}, role: {})", 
                   query.substring(0, Math.min(50, query.length())), searchTopK, role);
        
        return nlpWebClient.post()
                .uri("/search")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .doOnSuccess(response -> logger.info("Search completed successfully"))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        logger.error("NLP service error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    } else {
                        logger.error("Error searching NLP service: {}", error.getMessage());
                    }
                });
    }
    
    /**
     * Search for matching jobs without role filtering (backward compatibility)
     */
    public Mono<Map<String, Object>> search(String query, Integer topK) {
        return search(query, topK, null);
    }
    
    /**
     * Search with default top K
     */
    public Mono<Map<String, Object>> search(String query) {
        return search(query, defaultTopK);
    }
}

