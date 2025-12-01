#!/bin/bash

# Health Chat Advisor - Lambda Web Adapter Deployment Script

set -e

echo "========================================="
echo "Health Chat Advisor - Lambda Deployment"
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

if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

if ! command -v cdk &> /dev/null; then
    echo -e "${RED}Error: CDK is not installed${NC}"
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

# Test Docker build locally
echo -e "${BLUE}Step 3: Testing Docker build...${NC}"
docker build -t health-chat-advisor:test .

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Docker build failed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker build successful${NC}"

# Deploy with CDK
echo -e "${BLUE}Step 4: Deploying to AWS Lambda...${NC}"
cd cdk

# Set environment variables
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET set for deployment"

# Deploy
cdk deploy --require-approval never

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: CDK deployment failed${NC}"
    exit 1
fi

cd ..

echo ""
echo -e "${GREEN}=========================================${NC}"
echo -e "${GREEN}Lambda Deployment Complete!${NC}"
echo -e "${GREEN}=========================================${NC}"
echo ""
echo "Your application is now running on AWS Lambda!"
echo ""
echo "To get the API Gateway URL:"
echo "  aws cloudformation describe-stacks --stack-name HealthChatAdvisorStack --query 'Stacks[0].Outputs[?OutputKey==\`HealthChatApiEndpoint6F15432C\`].OutputValue' --output text"
echo ""
echo "To view logs:"
echo "  aws logs tail /aws/lambda/health-chat-webapp-* --follow"
echo ""
