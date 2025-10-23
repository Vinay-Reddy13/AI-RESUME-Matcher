package com.example.matcher.controller;

import com.example.matcher.dto.JobDto;
import com.example.matcher.entity.Job;
import com.example.matcher.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for Job operations
 */
@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "Job management operations")
public class JobController {
    
    private final JobService jobService;
    
    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }
    
    /**
     * Get all jobs
     */
    @GetMapping
    @Operation(summary = "Get all jobs", description = "Retrieve a list of all available jobs")
    public ResponseEntity<List<JobDto>> getAllJobs() {
        List<Job> jobs = jobService.getAllJobs();
        List<JobDto> jobDtos = jobs.stream()
                .map(JobDto::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDtos);
    }
    
    /**
     * Get job by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieve a specific job by its ID")
    public ResponseEntity<JobDto> getJobById(
            @Parameter(description = "Job ID") @PathVariable Long id) {
        
        Optional<Job> job = jobService.getJobById(id);
        
        if (job.isPresent()) {
            return ResponseEntity.ok(new JobDto(job.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Search jobs by company
     */
    @GetMapping("/search/company/{company}")
    @Operation(summary = "Search jobs by company", description = "Find jobs by company name")
    public ResponseEntity<List<JobDto>> getJobsByCompany(
            @Parameter(description = "Company name") @PathVariable String company) {
        
        List<Job> jobs = jobService.getJobsByCompany(company);
        List<JobDto> jobDtos = jobs.stream()
                .map(JobDto::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDtos);
    }
    
    /**
     * Search jobs by location
     */
    @GetMapping("/search/location/{location}")
    @Operation(summary = "Search jobs by location", description = "Find jobs by location")
    public ResponseEntity<List<JobDto>> getJobsByLocation(
            @Parameter(description = "Location") @PathVariable String location) {
        
        List<Job> jobs = jobService.getJobsByLocation(location);
        List<JobDto> jobDtos = jobs.stream()
                .map(JobDto::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDtos);
    }
    
    /**
     * Search jobs by text
     */
    @GetMapping("/search")
    @Operation(summary = "Search jobs by text", description = "Search jobs by title, company, or location")
    public ResponseEntity<List<JobDto>> searchJobs(
            @Parameter(description = "Search term") @RequestParam String q) {
        
        List<Job> jobs = jobService.searchJobs(q);
        List<JobDto> jobDtos = jobs.stream()
                .map(JobDto::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(jobDtos);
    }
    
    /**
     * Get job count
     */
    @GetMapping("/count")
    @Operation(summary = "Get job count", description = "Get the total number of jobs in the database")
    public ResponseEntity<Long> getJobCount() {
        long count = jobService.countJobs();
        return ResponseEntity.ok(count);
    }
}

