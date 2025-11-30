package com.health.chat.cdk;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

/**
 * AWS CDK Application entry point for Health Chat Advisor
 */
public class HealthChatCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        // Create the main stack
        new HealthChatStack(app, "HealthChatAdvisorStack", StackProps.builder()
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                .description("Health Chat Advisor Infrastructure")
                .build());

        app.synth();
    }
}
