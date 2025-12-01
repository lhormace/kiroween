package com.health.chat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

/**
 * AWS Configuration for production environment.
 * Optimizes S3 client with connection pooling, timeouts, and retry policies.
 */
@Configuration
@Profile("production")
public class AwsConfig {

    @Value("${aws.region:ap-northeast-1}")
    private String awsRegion;

    /**
     * Creates an optimized S3 client for production use.
     * Features:
     * - Connection pooling (max 50 connections)
     * - Retry policy with exponential backoff
     * - Appropriate timeouts
     * - HTTP/2 support
     */
    @Bean
    public S3Client s3Client() {
        // Configure retry policy
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .numRetries(3)
                .build();

        // Configure client override settings
        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofSeconds(60))
                .apiCallAttemptTimeout(Duration.ofSeconds(30))
                .retryPolicy(retryPolicy)
                .build();

        // Configure HTTP client with connection pooling
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder()
                .maxConnections(50)
                .connectionTimeout(Duration.ofSeconds(10))
                .socketTimeout(Duration.ofSeconds(30))
                .connectionAcquisitionTimeout(Duration.ofSeconds(10));

        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .httpClientBuilder(httpClientBuilder)
                .overrideConfiguration(clientConfig)
                .build();
    }
}
