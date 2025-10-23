package com.example.matcher.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Job entity representing job postings in the database
 */
@Entity
@Table(name = "jobs")
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;
    
    @NotBlank(message = "Company is required")
    @Column(nullable = false)
    private String company;
    
    @NotBlank(message = "Location is required")
    @Column(nullable = false)
    private String location;
    
    @NotBlank(message = "Job description is required")
    @Column(name = "jd_text", columnDefinition = "TEXT", nullable = false)
    private String jdText;
    
    // Default constructor
    public Job() {}
    
    // Constructor with all fields
    public Job(String title, String company, String location, String jdText) {
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
        return "Job{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}

