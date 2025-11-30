# AWS Lambda Handlers

このディレクトリには、健康管理チャットアドバイザーシステムのAWS Lambda関数ハンドラーが含まれています。

## ハンドラークラス

### 1. AuthHandler
認証関連の処理を担当するLambda関数。

**エンドポイント:**
- `POST /auth/login` - ユーザーログイン
- `POST /auth/validate` - トークン検証
- `POST /auth/logout` - ログアウト

**環境変数:**
- `S3_BUCKET_NAME` (必須): S3バケット名（デフォルト: "health-chat-data"）
- `AWS_REGION` (オプション): AWSリージョン（デフォルト: "us-east-1"）
- `JWT_SECRET` (必須): JWT署名用のシークレットキー

**リクエスト例:**
```json
// Login
POST /auth/login
{
  "username": "user@example.com",
  "password": "password123"
}

// Validate
POST /auth/validate
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

// Logout
POST /auth/logout
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 2. ChatHandler
対話処理を担当するLambda関数。メッセージの解析、健康データの抽出、アドバイス生成を行います。

**エンドポイント:**
- `POST /chat/message` - メッセージ処理

**環境変数:**
- `S3_BUCKET_NAME` (必須): S3バケット名（デフォルト: "health-chat-data"）
- `AWS_REGION` (オプション): AWSリージョン（デフォルト: "us-east-1"）
- `JWT_SECRET` (必須): JWT署名用のシークレットキー
- `MCP_ENDPOINT` (オプション): MCPサーバーのエンドポイントURL
- `MCP_TIMEOUT` (オプション): MCPリクエストのタイムアウト秒数（デフォルト: 10）

**認証:**
リクエストヘッダーに `Authorization: Bearer <token>` を含める必要があります。

**リクエスト例:**
```json
POST /chat/message
Headers: {
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
Body: {
  "message": "今日は体重65kg、朝食にご飯と卵を食べました"
}
```

### 3. AnalysisHandler
分析処理を担当するLambda関数。栄養素推定、短歌生成、履歴データの取得を行います。

**エンドポイント:**
- `POST /analysis/nutrition` - 栄養分析
- `POST /analysis/tanka` - 短歌生成
- `GET /analysis/tanka/history` - 短歌履歴取得
- `GET /analysis/daily` - 日次分析データ取得

**環境変数:**
- `S3_BUCKET_NAME` (必須): S3バケット名（デフォルト: "health-chat-data"）
- `AWS_REGION` (オプション): AWSリージョン（デフォルト: "us-east-1"）
- `JWT_SECRET` (必須): JWT署名用のシークレットキー

**認証:**
リクエストヘッダーに `Authorization: Bearer <token>` を含める必要があります。

**リクエスト例:**
```json
// Nutrition Analysis
POST /analysis/nutrition
Headers: {
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
Body: {
  "date": "2025-11-30"
}

// Tanka Generation
POST /analysis/tanka
Headers: {
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
Body: {
  "date": "2025-11-30"
}

// Daily Analysis
GET /analysis/daily?date=2025-11-30
Headers: {
  "Authorization": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

## デプロイ

### ビルド
```bash
mvn clean package
```

### Lambda関数の作成
AWS CDKまたはAWS CLIを使用してLambda関数を作成します。

**AWS CLI例:**
```bash
# AuthHandler
aws lambda create-function \
  --function-name health-chat-auth \
  --runtime java21 \
  --handler com.health.chat.lambda.AuthHandler \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-execution-role \
  --zip-file fileb://target/health-chat-advisor-1.0.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{S3_BUCKET_NAME=health-chat-data,JWT_SECRET=your-secret-key}"

# ChatHandler
aws lambda create-function \
  --function-name health-chat-chat \
  --runtime java21 \
  --handler com.health.chat.lambda.ChatHandler \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-execution-role \
  --zip-file fileb://target/health-chat-advisor-1.0.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{S3_BUCKET_NAME=health-chat-data,JWT_SECRET=your-secret-key,MCP_ENDPOINT=http://mcp-server:3000}"

# AnalysisHandler
aws lambda create-function \
  --function-name health-chat-analysis \
  --runtime java21 \
  --handler com.health.chat.lambda.AnalysisHandler \
  --role arn:aws:iam::ACCOUNT_ID:role/lambda-execution-role \
  --zip-file fileb://target/health-chat-advisor-1.0.0-SNAPSHOT.jar \
  --timeout 30 \
  --memory-size 512 \
  --environment Variables="{S3_BUCKET_NAME=health-chat-data,JWT_SECRET=your-secret-key}"
```

### IAM権限
Lambda実行ロールには以下の権限が必要です：
- S3への読み書き権限（`s3:GetObject`, `s3:PutObject`, `s3:ListBucket`）
- CloudWatch Logsへの書き込み権限

**IAMポリシー例:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::health-chat-data",
        "arn:aws:s3:::health-chat-data/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

## テスト

ローカルでのテスト用に、各ハンドラーにはテスト用のコンストラクタが用意されています。

```java
// Example test
AuthenticationService mockAuthService = mock(AuthenticationService.class);
AuthHandler handler = new AuthHandler(mockAuthService);

APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
request.setPath("/auth/login");
request.setHttpMethod("POST");
request.setBody("{\"username\":\"test\",\"password\":\"pass\"}");

APIGatewayProxyResponseEvent response = handler.handleRequest(request, mockContext);
```

## エラーハンドリング

すべてのハンドラーは以下のHTTPステータスコードを返します：
- `200` - 成功
- `400` - 不正なリクエスト
- `401` - 認証エラー
- `404` - リソースが見つからない
- `500` - サーバーエラー

エラーレスポンスの形式：
```json
{
  "error": "エラーの種類",
  "message": "詳細なエラーメッセージ"
}
```

## ログ

すべてのハンドラーはCloudWatch Logsにログを出力します。ログレベル：
- INFO: 正常な処理フロー
- WARNING: 警告（処理は継続）
- SEVERE: エラー（処理失敗）
