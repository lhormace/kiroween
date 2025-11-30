package com.health.chat.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.s3.StorageClass;
import software.amazon.awscdk.services.s3.Transition;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * AWS CDK Stack for Health Chat Advisor Infrastructure
 * 
 * This stack defines:
 * - S3 bucket for data storage with lifecycle policies
 * - Lambda functions for authentication, chat, and analysis
 * - API Gateway for REST API endpoints
 * - IAM roles and permissions
 */
public class HealthChatStack extends Stack {
    
    public HealthChatStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // S3 Bucket for data storage
        Bucket dataBucket = createDataBucket();

        // Lambda Functions
        Function authLambda = createAuthLambda(dataBucket);
        Function chatLambda = createChatLambda(dataBucket);
        Function analysisLambda = createAnalysisLambda(dataBucket);

        // API Gateway
        createApiGateway(authLambda, chatLambda, analysisLambda);
    }

    /**
     * Create S3 bucket with lifecycle policies for cost optimization
     */
    private Bucket createDataBucket() {
        return Bucket.Builder.create(this, "HealthDataBucket")
                .bucketName("health-chat-data-" + this.getAccount())
                .versioned(false)
                .lifecycleRules(List.of(
                        LifecycleRule.builder()
                                .transitions(List.of(
                                        Transition.builder()
                                                .storageClass(StorageClass.INFREQUENT_ACCESS)
                                                .transitionAfter(Duration.days(90))
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    /**
     * Create Authentication Lambda function
     */
    private Function createAuthLambda(Bucket dataBucket) {
        Function authLambda = Function.Builder.create(this, "AuthLambda")
                .functionName("health-chat-auth")
                .runtime(Runtime.JAVA_21)
                .handler("com.health.chat.lambda.AuthHandler::handleRequest")
                .code(Code.fromAsset("../target/health-chat-advisor-1.0.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(30))
                .memorySize(512)
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(Map.of(
                        "BUCKET_NAME", dataBucket.getBucketName(),
                        "JWT_SECRET", System.getenv().getOrDefault("JWT_SECRET", "change-me-in-production")
                ))
                .build();

        // Grant S3 permissions
        dataBucket.grantReadWrite(authLambda);

        return authLambda;
    }

    /**
     * Create Chat Lambda function
     */
    private Function createChatLambda(Bucket dataBucket) {
        Function chatLambda = Function.Builder.create(this, "ChatLambda")
                .functionName("health-chat-chat")
                .runtime(Runtime.JAVA_21)
                .handler("com.health.chat.lambda.ChatHandler::handleRequest")
                .code(Code.fromAsset("../target/health-chat-advisor-1.0.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(30))
                .memorySize(512)
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(Map.of(
                        "BUCKET_NAME", dataBucket.getBucketName(),
                        "JWT_SECRET", System.getenv().getOrDefault("JWT_SECRET", "change-me-in-production"),
                        "MCP_ENDPOINT", System.getenv().getOrDefault("MCP_ENDPOINT", "http://localhost:8080")
                ))
                .build();

        // Grant S3 permissions
        dataBucket.grantReadWrite(chatLambda);

        return chatLambda;
    }

    /**
     * Create Analysis Lambda function
     */
    private Function createAnalysisLambda(Bucket dataBucket) {
        Function analysisLambda = Function.Builder.create(this, "AnalysisLambda")
                .functionName("health-chat-analysis")
                .runtime(Runtime.JAVA_21)
                .handler("com.health.chat.lambda.AnalysisHandler::handleRequest")
                .code(Code.fromAsset("../target/health-chat-advisor-1.0.0-SNAPSHOT.jar"))
                .timeout(Duration.seconds(30))
                .memorySize(512)
                .logRetention(RetentionDays.ONE_MONTH)
                .environment(Map.of(
                        "BUCKET_NAME", dataBucket.getBucketName(),
                        "MCP_ENDPOINT", System.getenv().getOrDefault("MCP_ENDPOINT", "http://localhost:8080")
                ))
                .build();

        // Grant S3 permissions
        dataBucket.grantReadWrite(analysisLambda);

        return analysisLambda;
    }

    /**
     * Create API Gateway with routing to Lambda functions
     */
    private RestApi createApiGateway(Function authLambda, Function chatLambda, Function analysisLambda) {
        // Create REST API
        RestApi api = RestApi.Builder.create(this, "HealthChatApi")
                .restApiName("Health Chat API")
                .description("API for Health Chat Advisor")
                .deployOptions(StageOptions.builder()
                        .stageName("prod")
                        .loggingLevel(MethodLoggingLevel.INFO)
                        .dataTraceEnabled(true)
                        .build())
                .defaultCorsPreflightOptions(CorsOptions.builder()
                        .allowOrigins(Cors.ALL_ORIGINS)
                        .allowMethods(Cors.ALL_METHODS)
                        .build())
                .build();

        // Lambda integrations
        LambdaIntegration authIntegration = new LambdaIntegration(authLambda);
        LambdaIntegration chatIntegration = new LambdaIntegration(chatLambda);
        LambdaIntegration analysisIntegration = new LambdaIntegration(analysisLambda);

        // /auth endpoints
        Resource authResource = api.getRoot().addResource("auth");
        authResource.addResource("login").addMethod("POST", authIntegration);
        authResource.addResource("logout").addMethod("POST", authIntegration);
        authResource.addResource("validate").addMethod("POST", authIntegration);

        // /chat endpoint
        Resource chatResource = api.getRoot().addResource("chat");
        chatResource.addMethod("POST", chatIntegration, MethodOptions.builder()
                .authorizationType(AuthorizationType.CUSTOM)
                .build());

        // /analysis endpoints
        Resource analysisResource = api.getRoot().addResource("analysis");
        analysisResource.addResource("nutrition").addMethod("GET", analysisIntegration);
        analysisResource.addResource("mental").addMethod("GET", analysisIntegration);
        analysisResource.addResource("tanka").addMethod("GET", analysisIntegration);
        analysisResource.addResource("graph").addMethod("GET", analysisIntegration);

        return api;
    }
}
