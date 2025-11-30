#!/bin/bash

# Health Chat Advisor - Prerequisites Check Script
# このスクリプトは、デプロイに必要な前提条件をチェックします

echo "=========================================="
echo "Health Chat Advisor - 前提条件チェック"
echo "=========================================="
echo ""

# 色の定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# チェック結果のカウンター
PASSED=0
FAILED=0
WARNINGS=0

# Java 21のチェック
echo -n "Java 21をチェック中... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 21 ]; then
        echo -e "${GREEN}✓ OK${NC} ($(java -version 2>&1 | head -n 1))"
        ((PASSED++))
    else
        echo -e "${RED}✗ NG${NC} (Java 21以上が必要です)"
        ((FAILED++))
    fi
else
    echo -e "${RED}✗ NG${NC} (Javaがインストールされていません)"
    ((FAILED++))
fi

# Mavenのチェック
echo -n "Mavenをチェック中... "
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✓ OK${NC} ($MVN_VERSION)"
    ((PASSED++))
else
    echo -e "${RED}✗ NG${NC} (Mavenがインストールされていません)"
    ((FAILED++))
fi

# AWS CLIのチェック
echo -n "AWS CLIをチェック中... "
if command -v aws &> /dev/null; then
    AWS_VERSION=$(aws --version 2>&1)
    echo -e "${GREEN}✓ OK${NC} ($AWS_VERSION)"
    ((PASSED++))
else
    echo -e "${RED}✗ NG${NC} (AWS CLIがインストールされていません)"
    ((FAILED++))
fi

# AWS認証情報のチェック
echo -n "AWS認証情報をチェック中... "
if aws sts get-caller-identity &> /dev/null; then
    AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
    AWS_REGION=$(aws configure get region)
    echo -e "${GREEN}✓ OK${NC} (Account: $AWS_ACCOUNT, Region: $AWS_REGION)"
    ((PASSED++))
else
    echo -e "${RED}✗ NG${NC} (AWS認証情報が設定されていません)"
    echo "  'aws configure'を実行して設定してください"
    ((FAILED++))
fi

# Node.jsのチェック
echo -n "Node.jsをチェック中... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓ OK${NC} ($NODE_VERSION)"
    ((PASSED++))
else
    echo -e "${RED}✗ NG${NC} (Node.jsがインストールされていません)"
    ((FAILED++))
fi

# CDK CLIのチェック
echo -n "AWS CDK CLIをチェック中... "
if command -v cdk &> /dev/null; then
    CDK_VERSION=$(cdk --version)
    echo -e "${GREEN}✓ OK${NC} ($CDK_VERSION)"
    ((PASSED++))
else
    echo -e "${RED}✗ NG${NC} (AWS CDK CLIがインストールされていません)"
    echo "  'npm install -g aws-cdk'を実行してインストールしてください"
    ((FAILED++))
fi

echo ""
echo "=========================================="
echo "環境変数のチェック"
echo "=========================================="
echo ""

# JWT_SECRETのチェック
echo -n "JWT_SECRETをチェック中... "
if [ -z "$JWT_SECRET" ]; then
    echo -e "${YELLOW}⚠ 警告${NC} (設定されていません)"
    echo "  デフォルト値が使用されますが、本番環境では必ず設定してください"
    ((WARNINGS++))
else
    echo -e "${GREEN}✓ OK${NC} (設定済み)"
    ((PASSED++))
fi

# MCP_ENDPOINTのチェック
echo -n "MCP_ENDPOINTをチェック中... "
if [ -z "$MCP_ENDPOINT" ]; then
    echo -e "${YELLOW}⚠ 警告${NC} (設定されていません)"
    echo "  デフォルト値 (http://localhost:8080) が使用されます"
    ((WARNINGS++))
else
    echo -e "${GREEN}✓ OK${NC} (設定済み: $MCP_ENDPOINT)"
    ((PASSED++))
fi

echo ""
echo "=========================================="
echo "Lambda JARファイルのチェック"
echo "=========================================="
echo ""

# Lambda JARファイルの存在チェック
echo -n "Lambda JARファイルをチェック中... "
if [ -f "../target/health-chat-advisor-1.0.0-SNAPSHOT.jar" ]; then
    JAR_SIZE=$(du -h ../target/health-chat-advisor-1.0.0-SNAPSHOT.jar | cut -f1)
    echo -e "${GREEN}✓ OK${NC} (サイズ: $JAR_SIZE)"
    ((PASSED++))
else
    echo -e "${YELLOW}⚠ 警告${NC} (JARファイルが見つかりません)"
    echo "  'mvn clean package'を実行してビルドしてください"
    ((WARNINGS++))
fi

echo ""
echo "=========================================="
echo "チェック結果"
echo "=========================================="
echo ""
echo -e "${GREEN}成功: $PASSED${NC}"
echo -e "${YELLOW}警告: $WARNINGS${NC}"
echo -e "${RED}失敗: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    if [ $WARNINGS -eq 0 ]; then
        echo -e "${GREEN}✓ すべてのチェックに合格しました！${NC}"
        echo "デプロイを開始できます: ./deploy.sh"
        exit 0
    else
        echo -e "${YELLOW}⚠ 警告がありますが、デプロイは可能です${NC}"
        echo "デプロイを開始する場合: ./deploy.sh"
        exit 0
    fi
else
    echo -e "${RED}✗ 必須の前提条件が満たされていません${NC}"
    echo "上記のエラーを修正してから再度実行してください"
    exit 1
fi
