package com.example.matcher.controller;

import com.example.matcher.dto.RecommendRequest;
import com.example.matcher.dto.RecommendResponse;
import com.example.matcher.service.RecommendService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RecommendController
 */
@ExtendWith(MockitoExtension.class)
class RecommendControllerTest {

    @Mock
    private RecommendService recommendService;

    private RecommendController recommendController;

    @BeforeEach
    void setUp() {
        recommendController = new RecommendController(recommendService);
    }

    @Test
    void testGetRecommendations_Success() {
        // Arrange
        RecommendRequest request = new RecommendRequest("Software engineer with Java experience", 3);
        
        RecommendResponse response = new RecommendResponse();
        response.setQuery("Software engineer with Java experience");
        response.setTotalResults(1);
        
        RecommendResponse.JobRecommendation recommendation = new RecommendResponse.JobRecommendation(
            1L, "Java Developer", "TechCorp", "San Francisco, CA", "Job description...", 
            "Full job description with more details...", "https://example.com/apply", 0.95
        );
        response.setRecommendations(List.of(recommendation));
        
        when(recommendService.getRecommendations(any(RecommendRequest.class)))
                .thenReturn(Mono.just(response));

        // Act & Assert
        StepVerifier.create(recommendController.getRecommendations(request))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                    assert responseEntity.getBody().getTotalResults() == 1;
                    assert responseEntity.getBody().getRecommendations().size() == 1;
                })
                .verifyComplete();
    }

    @Test
    void testGetRecommendations_ServiceError() {
        // Arrange
        RecommendRequest request = new RecommendRequest("Test resume", 5);
        
        when(recommendService.getRecommendations(any(RecommendRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        // Act & Assert
        StepVerifier.create(recommendController.getRecommendations(request))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR;
                })
                .verifyComplete();
    }

    @Test
    void testGetSimpleRecommendations() {
        // Arrange
        String resumeText = "Software engineer with Java experience";
        
        RecommendResponse response = new RecommendResponse();
        response.setQuery(resumeText);
        response.setTotalResults(0);
        response.setRecommendations(new ArrayList<>());
        
        when(recommendService.getRecommendations(any(RecommendRequest.class)))
                .thenReturn(Mono.just(response));

        // Act & Assert
        StepVerifier.create(recommendController.getSimpleRecommendations(resumeText))
                .assertNext(responseEntity -> {
                    assert responseEntity.getStatusCode() == HttpStatus.OK;
                    assert responseEntity.getBody() != null;
                })
                .verifyComplete();
    }
}

