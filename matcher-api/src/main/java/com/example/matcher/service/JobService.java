package com.example.matcher.service;

import com.example.matcher.entity.Job;
import com.example.matcher.repo.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class for Job entity operations
 */
@Service
@Transactional
public class JobService {
    
    private final JobRepository jobRepository;
    
    @Autowired
    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }
    
    /**
     * Get all jobs
     */
    @Transactional(readOnly = true)
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    
    /**
     * Get job by ID
     */
    @Transactional(readOnly = true)
    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }
    
    /**
     * Save a job
     */
    public Job saveJob(Job job) {
        return jobRepository.save(job);
    }
    
    /**
     * Save multiple jobs
     */
    public List<Job> saveJobs(List<Job> jobs) {
        return jobRepository.saveAll(jobs);
    }
    
    /**
     * Delete job by ID
     */
    public void deleteJob(Long id) {
        jobRepository.deleteById(id);
    }
    
    /**
     * Get jobs by company
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByCompany(String company) {
        return jobRepository.findByCompanyIgnoreCase(company);
    }
    
    /**
     * Get jobs by location
     */
    @Transactional(readOnly = true)
    public List<Job> getJobsByLocation(String location) {
        return jobRepository.findByLocationIgnoreCase(location);
    }
    
    /**
     * Search jobs by title, company, or location
     */
    @Transactional(readOnly = true)
    public List<Job> searchJobs(String searchTerm) {
        return jobRepository.searchJobs(searchTerm);
    }
    
    /**
     * Count total jobs
     */
    @Transactional(readOnly = true)
    public long countJobs() {
        return jobRepository.countAllJobs();
    }
    
    /**
     * Check if jobs exist in database
     */
    @Transactional(readOnly = true)
    public boolean hasJobs() {
        return jobRepository.count() > 0;
    }
}

