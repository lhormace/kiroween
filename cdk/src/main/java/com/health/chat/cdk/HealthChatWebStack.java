package com.health.chat.cdk;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.DockerImageCode;
import software.amazon.awscdk.services.lambda.DockerImageFunction;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.LifecycleRule;
import software.amazon.awscdk.services.s3.StorageClass;
import software.amazon.awscdk.services.s3.Transition;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * AWS CDK Stack for Health Chat Advisor using Lambda Web Adapter
 * 
 * This stack defines:
 * - S3 bucket for data storage
 * - Docker-based Lambda function running Spring Boot
 * - Lambda Function URL for direct access
 */
public class HealthChatWebStack extends Stack {
    
    public HealthChatWebStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // S3 Bucket for data storage
        Bucket dataBucket = createDataBucket();

        // Lambda Function with Docker (Lambda Web Adapter)
        DockerImageFunction webAppFunction = createWebAppFunction(dataBucket);

        // Create Function URL for direct access
        var functionUrl = webAppFunction.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .build());

        // Output the Function URL
        new software.amazon.awscdk.CfnOutput(this, "FunctionUrl", 
            software.amazon.awscdk.CfnOutputProps.builder()
                .value(functionUrl.getUrl())
                .description("Health Chat Advisor Application URL")
                .build());
    }

    /**
     * Create S3 bucket with lifecycle policies for cost optimization
     */
    private Bucket createDataBucket() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String bucketName = "health-chat-data-" + this.getAccount() + "-" + timestamp;
        
        return Bucket.Builder.create(this, "HealthDataBucket")
                .bucketName(bucketName)
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
     * Create Lambda function with Docker image (Lambda Web Adapter)
     */
    private DockerImageFunction createWebAppFunction(Bucket dataBucket) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        // Get the project root directory (parent of cdk directory)
        String projectRoot = System.getProperty("user.dir");
        if (projectRoot.endsWith("/cdk")) {
            projectRoot = projectRoot.substring(0, projectRoot.length() - 4);
        }
        
        DockerImageFunction function = DockerImageFunction.Builder.create(this, "WebAppFunction")
                .functionName("health-chat-webapp-" + timestamp)
                .code(DockerImageCode.fromImageAsset(projectRoot))
                .timeout(Duration.seconds(30))
                .memorySize(1024)
                .logRetention(RetentionDays.ONE_WEEK)
                .environment(Map.of(
                        "BUCKET_NAME", dataBucket.getBucketName(),
                        "JWT_SECRET", System.getenv().getOrDefault("JWT_SECRET", "change-me-in-production"),
                        "SPRING_PROFILES_ACTIVE", "production"
                ))
                .build();

        // Grant S3 permissions
        dataBucket.grantReadWrite(function);

        return function;
    }
}
