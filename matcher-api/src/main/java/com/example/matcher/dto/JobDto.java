package com.example.matcher.dto;

import com.example.matcher.entity.Job;

/**
 * Data Transfer Object for Job entity
 */
public class JobDto {
    
    private Long id;
    private String title;
    private String company;
    private String location;
    private String jdText;
    
    // Default constructor
    public JobDto() {}
    
    // Constructor from entity
    public JobDto(Job job) {
        this.id = job.getId();
        this.title = job.getTitle();
        this.company = job.getCompany();
        this.location = job.getLocation();
        this.jdText = job.getJdText();
    }
    
    // Constructor with all fields
    public JobDto(Long id, String title, String company, String location, String jdText) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.jdText = jdText;
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
    
    public String getJdText() {
        return jdText;
    }
    
    public void setJdText(String jdText) {
        this.jdText = jdText;
    }
    
    @Override
    public String toString() {
        return "JobDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

