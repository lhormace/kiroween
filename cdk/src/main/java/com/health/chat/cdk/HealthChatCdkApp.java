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

        // Environment configuration
        Environment env = Environment.builder()
                .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                .region(System.getenv("CDK_DEFAULT_REGION"))
                .build();

        // Create the main stack with Lambda Web Adapter
        new HealthChatWebStack(app, "HealthChatAdvisorStack", StackProps.builder()
                .env(env)
                .description("Health Chat Advisor Infrastructure with Lambda Web Adapter")
                .build());

        // Create EC2 Scheduler stack for auto-stop at 17:00 JST
        new Ec2SchedulerStack(app, "Ec2SchedulerStack", StackProps.builder()
                .env(env)
                .description("EC2 Auto-Stop Scheduler - Stops instances at 17:00 JST daily")
                .build());

        app.synth();
    }
}
