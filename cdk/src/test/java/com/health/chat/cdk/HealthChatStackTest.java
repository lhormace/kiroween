package com.health.chat.cdk;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Health Chat CDK Stack
 */
class HealthChatStackTest {

    @Test
    void testStackCreation() {
        App app = new App();
        HealthChatStack stack = new HealthChatStack(app, "TestStack", null);
        
        Template template = Template.fromStack(stack);
        
        // Verify S3 bucket is created
        template.resourceCountIs("AWS::S3::Bucket", 1);
        
        // Verify 3 Lambda functions are created
        template.resourceCountIs("AWS::Lambda::Function", 3);
        
        // Verify API Gateway is created
        template.resourceCountIs("AWS::ApiGateway::RestApi", 1);
        
        // Verify IAM roles are created (one for each Lambda)
        template.resourceCountIs("AWS::IAM::Role", 3);
    }

    @Test
    void testLambdaConfiguration() {
        App app = new App();
        HealthChatStack stack = new HealthChatStack(app, "TestStack", null);
        
        Template template = Template.fromStack(stack);
        
        // Verify Lambda runtime is Java 21
        template.hasResourceProperties("AWS::Lambda::Function", 
            java.util.Map.of(
                "Runtime", "java21",
                "Timeout", 30,
                "MemorySize", 512
            )
        );
    }

    @Test
    void testS3LifecyclePolicy() {
        App app = new App();
        HealthChatStack stack = new HealthChatStack(app, "TestStack", null);
        
        Template template = Template.fromStack(stack);
        
        // Verify S3 bucket has lifecycle rules
        template.hasResourceProperties("AWS::S3::Bucket",
            java.util.Map.of(
                "LifecycleConfiguration", java.util.Map.of(
                    "Rules", java.util.List.of(
                        java.util.Map.of(
                            "Status", "Enabled",
                            "Transitions", java.util.List.of(
                                java.util.Map.of(
                                    "StorageClass", "STANDARD_IA",
                                    "TransitionInDays", 90
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    void testApiGatewayEndpoints() {
        App app = new App();
        HealthChatStack stack = new HealthChatStack(app, "TestStack", null);
        
        Template template = Template.fromStack(stack);
        
        // Verify API Gateway resources are created
        // We expect: root, auth, login, logout, validate, chat, analysis, nutrition, mental, tanka, graph
        // That's 11 resources total
        template.resourceCountIs("AWS::ApiGateway::Resource", 11);
        
        // Verify methods are created
        // login, logout, validate, chat, nutrition, mental, tanka, graph = 8 methods
        template.resourceCountIs("AWS::ApiGateway::Method", 8);
    }
}
