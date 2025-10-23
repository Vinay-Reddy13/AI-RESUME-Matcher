package com.example.matcher.repo;

import com.example.matcher.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Job entity
 */
@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    
    /**
     * Find jobs by company name (case-insensitive)
     */
    List<Job> findByCompanyIgnoreCase(String company);
    
    /**
     * Find jobs by location (case-insensitive)
     */
    List<Job> findByLocationIgnoreCase(String location);
    
    /**
     * Find jobs by title containing the given text (case-insensitive)
     */
    List<Job> findByTitleContainingIgnoreCase(String title);
    
    /**
     * Search jobs by title, company, or location (case-insensitive)
     */
    @Query("SELECT j FROM Job j WHERE " +
           "LOWER(j.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(j.company) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(j.location) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Job> searchJobs(@Param("searchTerm") String searchTerm);
    
    /**
     * Count total number of jobs
     */
    @Query("SELECT COUNT(j) FROM Job j")
    long countAllJobs();
}

