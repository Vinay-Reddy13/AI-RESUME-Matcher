package com.example.matcher.service;

import com.example.matcher.dto.RecommendRequest;
import com.example.matcher.dto.RecommendResponse;
import com.example.matcher.entity.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RecommendService
 */
@ExtendWith(MockitoExtension.class)
class RecommendServiceTest {

    @Mock
    private NlpClient nlpClient;

    @Mock
    private JobService jobService;

    private RecommendService recommendService;

    @BeforeEach
    void setUp() {
        recommendService = new RecommendService(nlpClient, jobService);
    }

    @Test
    void testGetRecommendations_Success() {
        // Arrange
        RecommendRequest request = new RecommendRequest("Software engineer with Java experience", 3);
        
        // Mock NLP response
        Map<String, Object> nlpResponse = new HashMap<>();
        nlpResponse.put("status", "success");
        
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> result1 = new HashMap<>();
        result1.put("id", 1L);
        result1.put("title", "Java Developer");
        result1.put("company", "TechCorp");
        result1.put("location", "San Francisco, CA");
        result1.put("score", 0.95);
        results.add(result1);
        
        nlpResponse.put("results", results);
        
        // Mock job service response
        Job job = new Job("Java Developer", "TechCorp", "San Francisco, CA", "Full job description here...");
        job.setId(1L);
        
        when(nlpClient.search(any(String.class), anyInt())).thenReturn(Mono.just(nlpResponse));
        when(jobService.getJobById(1L)).thenReturn(Optional.of(job));

        // Act & Assert
        StepVerifier.create(recommendService.getRecommendations(request))
                .assertNext(response -> {
                    assert response.getRecommendations().size() == 1;
                    assert response.getTotalResults() == 1;
                    assert response.getQuery().equals("Software engineer with Java experience");
                    
                    RecommendResponse.JobRecommendation recommendation = response.getRecommendations().get(0);
                    assert recommendation.getId().equals(1L);
                    assert recommendation.getTitle().equals("Java Developer");
                    assert recommendation.getCompany().equals("TechCorp");
                    assert recommendation.getLocation().equals("San Francisco, CA");
                    assert recommendation.getScore().equals(0.95);
                    assert recommendation.getSnippet().contains("Full job description");
                })
                .verifyComplete();
    }

    @Test
    void testGetRecommendations_EmptyResults() {
        // Arrange
        RecommendRequest request = new RecommendRequest("Test resume", 5);
        
        Map<String, Object> nlpResponse = new HashMap<>();
        nlpResponse.put("status", "success");
        nlpResponse.put("results", new ArrayList<>());
        
        when(nlpClient.search(any(String.class), anyInt())).thenReturn(Mono.just(nlpResponse));

        // Act & Assert
        StepVerifier.create(recommendService.getRecommendations(request))
                .assertNext(response -> {
                    assert response.getRecommendations().isEmpty();
                    assert response.getTotalResults() == 0;
                })
                .verifyComplete();
    }

    @Test
    void testGetRecommendations_NlpServiceError() {
        // Arrange
        RecommendRequest request = new RecommendRequest("Test resume", 5);
        
        Map<String, Object> nlpResponse = new HashMap<>();
        nlpResponse.put("status", "error");
        nlpResponse.put("message", "NLP service error");
        
        when(nlpClient.search(any(String.class), anyInt())).thenReturn(Mono.just(nlpResponse));

        // Act & Assert
        StepVerifier.create(recommendService.getRecommendations(request))
                .assertNext(response -> {
                    assert response.getRecommendations().isEmpty();
                    assert response.getTotalResults() == 0;
                })
                .verifyComplete();
    }
}

