package com.example.matcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Spring Boot application class for AI Resume Matcher
 */
@SpringBootApplication
public class MatcherApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(MatcherApplication.class);
    
    @Autowired
    private Environment environment;
    
    public static void main(String[] args) {
        SpringApplication.run(MatcherApplication.class, args);
    }
    
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("AI Resume Matcher API started successfully");
        logger.info("NLP Service URL: {}", environment.getProperty("NLP_BASE_URL", "http://nlp:8001"));
        logger.info("Top K results: {}", environment.getProperty("TOP_K", "5"));
        logger.info("API Documentation available at: http://localhost:8080/swagger-ui/index.html");
    }
}

