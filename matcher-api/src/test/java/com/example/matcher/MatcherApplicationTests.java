package com.example.matcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic integration test for the Spring Boot application
 */
@SpringBootTest
@ActiveProfiles("test")
class MatcherApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies that the Spring context loads successfully
        // It's a basic smoke test to ensure the application can start
    }
}

