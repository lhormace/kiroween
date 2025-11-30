#!/bin/bash

# Health Chat Advisor - CDK Deployment Script
# このスクリプトは、Lambda関数のビルドとCDKスタックのデプロイを自動化します

set -e

echo "=========================================="
echo "Health Chat Advisor - CDK Deployment"
echo "=========================================="
echo ""

# 環境変数のチェック
if [ -z "$JWT_SECRET" ]; then
    echo "警告: JWT_SECRET環境変数が設定されていません"
    echo "デフォルト値が使用されますが、本番環境では必ず設定してください"
    echo ""
fi

if [ -z "$MCP_ENDPOINT" ]; then
    echo "警告: MCP_ENDPOINT環境変数が設定されていません"
    echo "デフォルト値 (http://localhost:8080) が使用されます"
    echo ""
fi

# Step 1: Lambda関数のビルド
echo "Step 1: Lambda関数をビルドしています..."
cd ..
mvn clean package -DskipTests
echo "✓ Lambda関数のビルドが完了しました"
echo ""

# Step 2: CDKプロジェクトのビルド
echo "Step 2: CDKプロジェクトをビルドしています..."
cd cdk
mvn compile
echo "✓ CDKプロジェクトのビルドが完了しました"
echo ""

# Step 3: CloudFormationテンプレートの生成
echo "Step 3: CloudFormationテンプレートを生成しています..."
cdk synth
echo "✓ CloudFormationテンプレートの生成が完了しました"
echo ""

# Step 4: デプロイ
echo "Step 4: AWSにデプロイしています..."
echo "このステップには数分かかる場合があります..."
cdk deploy --require-approval never
echo ""
echo "=========================================="
echo "✓ デプロイが完了しました！"
echo "=========================================="
echo ""
echo "API GatewayのエンドポイントURLは上記の出力を確認してください"
echo ""
