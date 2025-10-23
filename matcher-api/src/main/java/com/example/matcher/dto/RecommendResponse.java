package com.example.matcher.dto;

import java.util.List;

/**
 * Response DTO for job recommendations
 */
public class RecommendResponse {
    
    private List<JobRecommendation> recommendations;
    private int totalResults;
    private String query;
    
    // Default constructor
    public RecommendResponse() {}
    
    // Constructor with all fields
    public RecommendResponse(List<JobRecommendation> recommendations, int totalResults, String query) {
        this.recommendations = recommendations;
        this.totalResults = totalResults;
        this.query = query;
    }
    
    // Getters and Setters
    public List<JobRecommendation> getRecommendations() {
        return recommendations;
    }
    
    public void setRecommendations(List<JobRecommendation> recommendations) {
        this.recommendations = recommendations;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
    
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    public String getQuery() {
        return query;
    }
    
    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * Inner class for individual job recommendations
     */
    public static class JobRecommendation {
        private Long id;
        private String title;
        private String company;
        private String location;
        private String snippet;
        private String fullDescription;
        private String applicationUrl;
        private Double score;
        
        // Default constructor
        public JobRecommendation() {}
        
        // Constructor with all fields
        public JobRecommendation(Long id, String title, String company, String location, String snippet, String fullDescription, String applicationUrl, Double score) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.location = location;
            this.snippet = snippet;
            this.fullDescription = fullDescription;
            this.applicationUrl = applicationUrl;
            this.score = score;
        }
        
        // Getters and Setters
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public void setTitle(String title) {
            this.title = title;
        }
        
        public String getCompany() {
            return company;
        }
        
        public void setCompany(String company) {
            this.company = company;
        }
        
        public String getLocation() {
            return location;
        }
        
        public void setLocation(String location) {
            this.location = location;
        }
        
        public String getSnippet() {
            return snippet;
        }
        
        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }
        
        public Double getScore() {
            return score;
        }
        
        public void setScore(Double score) {
            this.score = score;
        }
        
        public String getFullDescription() {
            return fullDescription;
        }
        
        public void setFullDescription(String fullDescription) {
            this.fullDescription = fullDescription;
        }
        
        public String getApplicationUrl() {
            return applicationUrl;
        }
        
        public void setApplicationUrl(String applicationUrl) {
            this.applicationUrl = applicationUrl;
        }
        
        @Override
        public String toString() {
            return "JobRecommendation{" +
                    "id=" + id +
                    ", title='" + title + '\'' +
                    ", company='" + company + '\'' +
                    ", location='" + location + '\'' +
                    ", score=" + score +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "RecommendResponse{" +
                "recommendations=" + (recommendations != null ? recommendations.size() : 0) + " items" +
                ", totalResults=" + totalResults +
                ", query='" + (query != null ? query.substring(0, Math.min(50, query.length())) + "..." : "null") + '\'' +
                '}';
    }
}

