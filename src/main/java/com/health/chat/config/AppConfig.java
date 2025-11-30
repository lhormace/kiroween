package com.health.chat.config;

import com.health.chat.repository.DataRepository;
import com.health.chat.repository.S3DataRepository;
import com.health.chat.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AppConfig {

    @Value("${aws.s3.bucket-name:health-chat-data}")
    private String bucketName;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${local.mode:true}")
    private boolean localMode;

    @Bean
    public GraphGenerator graphGenerator() {
        return new ChartJsGraphGenerator();
    }

    @Bean
    public S3Client s3Client() {
        if (localMode) {
            // ローカルモードではS3クライアントを作成しない
            return null;
        }
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public DataRepository dataRepository(@Value("${local.data.directory:./data}") String localDataDirectory) {
        if (localMode) {
            // ローカルモードではファイルベースのリポジトリを使用
            return new com.health.chat.repository.LocalFileDataRepository(localDataDirectory);
        } else {
            // 本番モードではS3を使用
            S3Client client = S3Client.builder().region(Region.of(awsRegion)).build();
            return new S3DataRepository(client, bucketName);
        }
    }
    
    @Bean
    public MessageParser messageParser() {
        return new MessageParser();
    }
    
    @Bean
    public NutritionEstimator nutritionEstimator(DataRepository dataRepository) {
        return new BasicNutritionEstimator(dataRepository);
    }
    
    @Bean
    public MentalStateAnalyzer mentalStateAnalyzer() {
        return new KeywordBasedMentalStateAnalyzer();
    }
    
    @Bean
    public TankaGenerator tankaGenerator() {
        return new SimpleTankaGenerator();
    }
    
    @Bean
    public MCPClient mcpClient(@Value("${MCP_ENDPOINT:http://localhost:3000}") String mcpEndpoint) {
        return new HttpMCPClient(mcpEndpoint, 10);
    }
    
    @Bean
    public HealthAdvisorAI healthAdvisorAI(MCPClient mcpClient) {
        return new MCPBasedHealthAdvisor(mcpClient);
    }
}
