package com.example.matcher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for job recommendations
 */
public class RecommendRequest {
    
    @NotBlank(message = "Resume text is required")
    private String resumeText;
    
    @Positive(message = "Top K must be positive")
    private Integer topK = 5;
    
    private String role; // Optional role parameter for filtering
    
    // Default constructor
    public RecommendRequest() {}
    
    // Constructor with resume text
    public RecommendRequest(String resumeText) {
        this.resumeText = resumeText;
    }
    
    // Constructor with all fields
    public RecommendRequest(String resumeText, Integer topK) {
        this.resumeText = resumeText;
        this.topK = topK;
    }
    
    // Constructor with all fields including role
    public RecommendRequest(String resumeText, Integer topK, String role) {
        this.resumeText = resumeText;
        this.topK = topK;
        this.role = role;
    }
    
    // Getters and Setters
    public String getResumeText() {
        return resumeText;
    }
    
    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }
    
    public Integer getTopK() {
        return topK;
    }
    
    public void setTopK(Integer topK) {
        this.topK = topK;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    @Override
    public String toString() {
        return "RecommendRequest{" +
                "resumeText='" + (resumeText != null ? resumeText.substring(0, Math.min(50, resumeText.length())) + "..." : "null") + '\'' +
                ", topK=" + topK +
                ", role='" + role + '\'' +
                '}';
    }
}

