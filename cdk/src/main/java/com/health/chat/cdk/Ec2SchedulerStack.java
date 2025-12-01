package com.health.chat.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.Duration;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * CDK Stack for EC2 Instance Scheduler
 * 
 * Automatically stops EC2 instances at 17:00 JST (08:00 UTC) every day
 */
public class Ec2SchedulerStack extends Stack {
    
    public Ec2SchedulerStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Lambda function to stop EC2 instances
        Function stopEc2Function = createStopEc2Function();

        // EventBridge rule to trigger at 17:00 JST (08:00 UTC)
        Rule stopRule = Rule.Builder.create(this, "StopEc2Rule")
                .ruleName("stop-ec2-daily-17-00-jst")
                .description("Stop EC2 instances at 17:00 JST every day")
                .schedule(Schedule.cron(
                    software.amazon.awscdk.services.events.CronOptions.builder()
                        .minute("0")
                        .hour("8")  // 17:00 JST = 08:00 UTC
                        .build()
                ))
                .build();

        // Add Lambda as target
        stopRule.addTarget(LambdaFunction.Builder.create(stopEc2Function).build());

        // Output
        new software.amazon.awscdk.CfnOutput(this, "StopFunctionName",
            software.amazon.awscdk.CfnOutputProps.builder()
                .value(stopEc2Function.getFunctionName())
                .description("Lambda function that stops EC2 instances")
                .build());
    }

    /**
     * Create Lambda function to stop EC2 instances
     */
    private Function createStopEc2Function() {
        Function function = Function.Builder.create(this, "StopEc2Function")
                .functionName("ec2-auto-stop")
                .runtime(Runtime.PYTHON_3_11)
                .handler("index.handler")
                .code(Code.fromInline(
                    "import boto3\n" +
                    "import os\n" +
                    "\n" +
                    "ec2 = boto3.client('ec2')\n" +
                    "\n" +
                    "def handler(event, context):\n" +
                    "    # Get configuration from environment variables\n" +
                    "    instance_ids = os.environ.get('INSTANCE_IDS', '').split(',')\n" +
                    "    eb_env_name = os.environ.get('EB_ENVIRONMENT_NAME', '')\n" +
                    "    \n" +
                    "    filters = [{'Name': 'instance-state-name', 'Values': ['running']}]\n" +
                    "    \n" +
                    "    if instance_ids and instance_ids != ['']:\n" +
                    "        # Use specific instance IDs\n" +
                    "        print(f'Using specific instance IDs: {instance_ids}')\n" +
                    "    elif eb_env_name:\n" +
                    "        # Find Elastic Beanstalk instances by environment name\n" +
                    "        print(f'Finding Elastic Beanstalk instances for environment: {eb_env_name}')\n" +
                    "        filters.append({'Name': 'tag:elasticbeanstalk:environment-name', 'Values': [eb_env_name]})\n" +
                    "        instance_ids = []\n" +
                    "    else:\n" +
                    "        # Find instances with AutoStop tag\n" +
                    "        print('Finding instances with AutoStop=true tag')\n" +
                    "        filters.append({'Name': 'tag:AutoStop', 'Values': ['true']})\n" +
                    "        instance_ids = []\n" +
                    "    \n" +
                    "    # Query instances if not using specific IDs\n" +
                    "    if not instance_ids or instance_ids == ['']:\n" +
                    "        response = ec2.describe_instances(Filters=filters)\n" +
                    "        instance_ids = []\n" +
                    "        for reservation in response['Reservations']:\n" +
                    "            for instance in reservation['Instances']:\n" +
                    "                instance_ids.append(instance['InstanceId'])\n" +
                    "    \n" +
                    "    if instance_ids:\n" +
                    "        print(f'Stopping {len(instance_ids)} instances: {instance_ids}')\n" +
                    "        ec2.stop_instances(InstanceIds=instance_ids)\n" +
                    "        return {\n" +
                    "            'statusCode': 200,\n" +
                    "            'body': f'Successfully stopped {len(instance_ids)} instances: {instance_ids}'\n" +
                    "        }\n" +
                    "    else:\n" +
                    "        print('No running instances found to stop')\n" +
                    "        return {\n" +
                    "            'statusCode': 200,\n" +
                    "            'body': 'No running instances found to stop'\n" +
                    "        }\n"
                ))
                .timeout(Duration.seconds(60))
                .environment(Map.of(
                    // オプション1: 特定のインスタンスIDを指定
                    "INSTANCE_IDS", "",
                    // オプション2: Elastic Beanstalk環境名を指定（推奨）
                    "EB_ENVIRONMENT_NAME", "health-chat-env"
                    // オプション3: 空の場合はAutoStop=trueタグで検索
                ))
                .build();

        // Grant EC2 permissions
        function.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                    "ec2:DescribeInstances",
                    "ec2:StopInstances"
                ))
                .resources(List.of("*"))
                .build());

        return function;
    }
}
