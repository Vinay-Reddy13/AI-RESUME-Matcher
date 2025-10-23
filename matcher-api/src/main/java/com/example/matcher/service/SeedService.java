package com.example.matcher.service;

import com.example.matcher.entity.Job;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for seeding the database with job data from CSV
 */
@Service
public class SeedService {
    
    private static final Logger logger = LoggerFactory.getLogger(SeedService.class);
    
    private final JobService jobService;
    
    @Value("${app.data.path:/app/data}")
    private String dataPath;
    
    @Autowired
    public SeedService(JobService jobService) {
        this.jobService = jobService;
    }
    
    /**
     * Seed database on application startup if empty
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedDatabaseIfEmpty() {
        if (!jobService.hasJobs()) {
            logger.info("Database is empty, seeding with job data...");
            seedDatabase();
        } else {
            logger.info("Database already contains {} jobs, skipping seed", jobService.countJobs());
        }
    }
    
    /**
     * Seed database with jobs from CSV file
     */
    @Transactional
    public void seedDatabase() {
        try {
            String csvPath = dataPath + "/jobs.csv";
            logger.info("Loading jobs from CSV: {}", csvPath);
            
            if (!Files.exists(Paths.get(csvPath))) {
                logger.error("CSV file not found at: {}", csvPath);
                return;
            }
            
            List<Job> jobs = loadJobsFromCsv(csvPath);
            
            if (jobs.isEmpty()) {
                logger.warn("No jobs loaded from CSV file");
                return;
            }
            
            jobService.saveJobs(jobs);
            logger.info("Successfully seeded database with {} jobs", jobs.size());
            
        } catch (Exception e) {
            logger.error("Error seeding database: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Load jobs from CSV file
     */
    private List<Job> loadJobsFromCsv(String csvPath) throws IOException, CsvException {
        List<Job> jobs = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            List<String[]> records = reader.readAll();
            
            // Skip header row
            for (int i = 1; i < records.size(); i++) {
                String[] record = records.get(i);
                
                if (record.length >= 5) {
                    try {
                        Job job = new Job();
                        job.setId(Long.parseLong(record[0].trim()));
                        job.setTitle(record[1].trim());
                        job.setCompany(record[2].trim());
                        job.setLocation(record[3].trim());
                        job.setJdText(record[4].trim());
                        
                        jobs.add(job);
                        
                    } catch (NumberFormatException e) {
                        logger.warn("Skipping invalid record at line {}: {}", i + 1, String.join(",", record));
                    }
                } else {
                    logger.warn("Skipping incomplete record at line {}: {}", i + 1, String.join(",", record));
                }
            }
        }
        
        logger.info("Loaded {} valid job records from CSV", jobs.size());
        return jobs;
    }
    
    /**
     * Clear all jobs from database
     */
    @Transactional
    public void clearDatabase() {
        logger.info("Clearing all jobs from database");
        // Note: This would require implementing deleteAll in JobService
        // For now, this is a placeholder
    }
}

