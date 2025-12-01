#!/bin/bash

# Health Chat Advisor - Elastic Beanstalk Deployment Script

set -e

echo "========================================="
echo "Health Chat Advisor - EB Deployment"
echo "========================================="

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check prerequisites
echo -e "${BLUE}Step 1: Checking prerequisites...${NC}"

if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: AWS CLI is not installed${NC}"
    exit 1
fi

if ! command -v eb &> /dev/null; then
    echo -e "${RED}Error: EB CLI is not installed${NC}"
    echo "Install with: pip install awsebcli"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites met${NC}"

# Build application
echo -e "${BLUE}Step 2: Building application...${NC}"
mvn clean package -DskipTests

if [ ! -f "target/health-chat-advisor-1.0.0-SNAPSHOT-exec.jar" ]; then
    echo -e "${RED}Error: Build failed - JAR file not found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Application built successfully${NC}"

# Initialize EB (if not already initialized)
if [ ! -d ".elasticbeanstalk" ]; then
    echo -e "${BLUE}Step 3: Initializing Elastic Beanstalk...${NC}"
    eb init health-chat-advisor --platform "Corretto 17" --region ap-northeast-1
    echo -e "${GREEN}✓ EB initialized${NC}"
else
    echo -e "${GREEN}✓ EB already initialized${NC}"
fi

# Create environment (if not exists)
echo -e "${BLUE}Step 4: Checking EB environment...${NC}"

if ! eb list | grep -q "health-chat-env"; then
    echo -e "${BLUE}Creating new environment (single instance, no auto-scaling)...${NC}"
    eb create health-chat-env \
        --single \
        --instance-type t3.small \
        --envvars JWT_SECRET=$(openssl rand -base64 32)
    echo -e "${GREEN}✓ Environment created${NC}"
else
    echo -e "${GREEN}✓ Environment exists${NC}"
fi

# Deploy
echo -e "${BLUE}Step 5: Deploying application...${NC}"
eb deploy health-chat-env --timeout 20

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Deployment failed${NC}"
    echo "Check logs with: eb logs health-chat-env"
    exit 1
fi

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Deployment Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "Your application is now running on AWS Elastic Beanstalk!"
echo ""
echo "Useful commands:"
echo "  eb open health-chat-env          - Open application in browser"
echo "  eb logs health-chat-env          - View application logs"
echo "  eb status health-chat-env        - Check environment status"
echo "  eb health health-chat-env        - Check instance health"
echo "  eb ssh health-chat-env           - SSH into instance"
echo ""
echo "Monitoring:"
echo "  CloudWatch Logs: https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-1#logsV2:log-groups"
echo "  EB Console: https://console.aws.amazon.com/elasticbeanstalk/home?region=ap-northeast-1"
echo ""
