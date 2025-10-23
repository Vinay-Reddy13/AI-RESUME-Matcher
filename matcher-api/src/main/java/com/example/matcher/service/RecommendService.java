package com.example.matcher.service;

import com.example.matcher.dto.RecommendRequest;
import com.example.matcher.dto.RecommendResponse;
import com.example.matcher.entity.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Service for generating job recommendations using NLP service
 */
@Service
public class RecommendService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendService.class);
    
    private final NlpClient nlpClient;
    private final JobService jobService;
    
    @Autowired
    public RecommendService(NlpClient nlpClient, JobService jobService) {
        this.nlpClient = nlpClient;
        this.jobService = jobService;
    }
    
    /**
     * Get job recommendations based on resume text
     */
    public Mono<RecommendResponse> getRecommendations(RecommendRequest request) {
        logger.info("Getting recommendations for resume text: {} characters, role: {}", 
                   request.getResumeText().length(), request.getRole());
        
        return nlpClient.search(request.getResumeText(), request.getTopK(), request.getRole())
                .map(nlpResponse -> {
                    try {
                        return processNlpResponse(nlpResponse, request.getResumeText());
                    } catch (Exception e) {
                        logger.error("Error processing NLP response: {}", e.getMessage());
                        throw new RuntimeException("Failed to process recommendations", e);
                    }
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    logger.error("NLP service error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    return Mono.just(createErrorResponse(request.getResumeText(), "NLP service unavailable"));
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Unexpected error: {}", ex.getMessage());
                    return Mono.just(createErrorResponse(request.getResumeText(), "Internal server error"));
                });
    }
    
    /**
     * Process NLP service response and create recommendations
     */
    private RecommendResponse processNlpResponse(Map<String, Object> nlpResponse, String query) {
        if (!"success".equals(nlpResponse.get("status"))) {
            logger.error("NLP service returned error: {}", nlpResponse.get("message"));
            return createErrorResponse(query, "NLP service error: " + nlpResponse.get("message"));
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) nlpResponse.get("results");
        String detectedRole = (String) nlpResponse.get("role");
        
        if (results == null || results.isEmpty()) {
            logger.warn("No results returned from NLP service for role: {}", detectedRole);
            return new RecommendResponse(new ArrayList<>(), 0, query);
        }
        
        List<RecommendResponse.JobRecommendation> recommendations = results.stream()
                .map(this::createJobRecommendation)
                .collect(Collectors.toList());
        
        logger.info("Created {} recommendations for role: {}", recommendations.size(), detectedRole);
        return new RecommendResponse(recommendations, recommendations.size(), query);
    }
    
    /**
     * Create a job recommendation from NLP result
     */
    private RecommendResponse.JobRecommendation createJobRecommendation(Map<String, Object> result) {
        Long id = Long.valueOf(result.get("id").toString());
        String title = result.get("title").toString();
        String company = result.get("company").toString();
        String location = result.get("location").toString();
        Double score = Double.valueOf(result.get("score").toString());
        
        // Get full job details from database
        Job job = jobService.getJobById(id).orElse(null);
        String snippet = job != null ? createSnippet(job.getJdText()) : "Job description not available";
        String fullDescription = job != null ? job.getJdText() : "Job description not available";
        String applicationUrl = generateApplicationUrl(company, title, id);
        
        return new RecommendResponse.JobRecommendation(id, title, company, location, snippet, fullDescription, applicationUrl, score);
    }
    
    /**
     * Create a snippet from job description (first 200 characters)
     */
    private String createSnippet(String jdText) {
        if (jdText == null || jdText.isEmpty()) {
            return "No description available";
        }
        
        String snippet = jdText.trim();
        if (snippet.length() > 200) {
            snippet = snippet.substring(0, 200) + "...";
        }
        
        return snippet;
    }
    
    /**
     * Generate application URL for a job posting using real job search APIs
     */
    private String generateApplicationUrl(String company, String title, Long jobId) {
        String companyLower = company.toLowerCase().trim();
        String jobTitle = title.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
        
        // Create search-friendly terms
        String searchTitle = jobTitle.replaceAll("\\s+", "%20");
        
        // Map to real careers pages and search functionality
        switch (companyLower) {
            case "amazon":
                return "https://amazon.jobs/en/search?keywords=" + searchTitle + "&location=&business_category[]=all&category[]=all&schedule_type_id[]=all";
                
            case "google":
            case "alphabet":
                return "https://careers.google.com/jobs/results/?q=" + searchTitle;
                
            case "microsoft":
                return "https://careers.microsoft.com/us/en/search-results?keywords=" + searchTitle;
                
            case "meta":
            case "facebook":
                return "https://www.metacareers.com/jobs/?q=" + searchTitle;
                
            case "apple":
                return "https://jobs.apple.com/en-us/search?search=" + searchTitle;
                
            case "netflix":
                return "https://jobs.netflix.com/search?q=" + searchTitle;
                
            case "salesforce":
                return "https://salesforce.wd1.myworkdayjobs.com/en-US/External_Career_Site?q=" + searchTitle;
                
            case "oracle":
                return "https://www.oracle.com/corporate/careers/search/?keyword=" + searchTitle;
                
            case "cisco":
                return "https://jobs.cisco.com/jobs/SearchJobs/?21178=%5B%5D&21178_format=3572&listFilterMode=1&keywords=" + searchTitle;
                
            case "ibm":
                return "https://www.ibm.com/careers/search?q=" + searchTitle;
                
            case "jp morgan chase":
            case "jpmorgan chase":
            case "jpmorgan":
                return "https://careers.jpmorganchase.com/us/en/search-results?keywords=" + searchTitle;
                
            case "goldman sachs":
                return "https://www.goldmansachs.com/careers/featured-opportunities/";
                
            case "mphasis":
                return "https://careers.mphasis.com/us/en/search-results?keywords=" + searchTitle;
                
            case "shopify":
                return "https://www.shopify.com/careers/search?keywords=" + searchTitle;
                
            default:
                // Use LinkedIn for better job search results
                return generateLinkedInJobSearchUrl(jobTitle, company);
        }
    }
    
    /**
     * Generate LinkedIn job search URL with better formatting
     */
    private String generateLinkedInJobSearchUrl(String title, String company) {
        String searchQuery = (title + " " + company)
            .replaceAll("[^a-zA-Z0-9\\s]", "")
            .replaceAll("\\s+", " ")
            .trim()
            .replaceAll("\\s", "%20");
            
        return "https://www.linkedin.com/jobs/search/?keywords=" + searchQuery + "&position=1&pageNum=0";
    }
    
    /**
     * Create error response
     */
    private RecommendResponse createErrorResponse(String query, String errorMessage) {
        logger.error("Creating error response: {}", errorMessage);
        return new RecommendResponse(new ArrayList<>(), 0, query);
    }
}

