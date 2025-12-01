package com.health.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/**
 * Main application class for Health Chat Advisor.
 * Configured for production deployment on AWS Elastic Beanstalk.
 */
@SpringBootApplication
@EnableConfigurationProperties
@EnableAsync
public class HealthChatApplication {
    
    public static void main(String[] args) {
        // Set default timezone to JST for consistent date handling
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        
        // Configure Spring Boot application
        SpringApplication app = new SpringApplication(HealthChatApplication.class);
        
        // Set default profile if not specified
        app.setAdditionalProfiles("production");
        
        app.run(args);
    }
}
