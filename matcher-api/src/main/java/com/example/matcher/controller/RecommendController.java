package com.example.matcher.controller;

import com.example.matcher.dto.RecommendRequest;
import com.example.matcher.dto.RecommendResponse;
import com.example.matcher.service.RecommendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for job recommendations
 */
@RestController
@RequestMapping("/api/recommend")
@Tag(name = "Recommendations", description = "AI-powered job recommendation operations")
public class RecommendController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendController.class);
    
    private final RecommendService recommendService;
    
    @Autowired
    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }
    
    /**
     * Get job recommendations based on resume text
     */
    @PostMapping
    @Operation(
        summary = "Get job recommendations",
        description = "Get AI-powered job recommendations based on resume text using semantic search",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Resume text and search parameters",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = RecommendRequest.class),
                examples = @ExampleObject(
                    value = "{\"resumeText\": \"Software engineer with 3 years experience in Java and Spring Boot\", \"topK\": 5}"
                )
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Recommendations retrieved successfully",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RecommendResponse.class)
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body"
            ),
            @ApiResponse(
                responseCode = "500",
                description = "Internal server error or NLP service unavailable"
            )
        }
    )
    public Mono<ResponseEntity<RecommendResponse>> getRecommendations(
            @Valid @RequestBody RecommendRequest request) {
        
        return recommendService.getRecommendations(request)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error processing recommendation request: {}", error.getMessage(), error);
                    RecommendResponse errorResponse = new RecommendResponse(
                        new ArrayList<>(), 0, request.getResumeText());
                    return Mono.just(ResponseEntity.status(500)
                        .body(errorResponse));
                });
    }
    
    /**
     * Get job recommendations with default parameters
     */
    @PostMapping("/simple")
    @Operation(
        summary = "Get job recommendations (simple)",
        description = "Get job recommendations with just resume text (uses default topK=5)"
    )
    public Mono<ResponseEntity<RecommendResponse>> getSimpleRecommendations(
            @RequestBody String resumeText) {
        
        RecommendRequest request = new RecommendRequest(resumeText);
        return getRecommendations(request);
    }
}

