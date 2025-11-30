# Health Chat Advisor - デプロイメントガイド

このドキュメントでは、Health Chat AdvisorシステムをAWSにデプロイする手順を説明します。

## 目次

1. [前提条件](#前提条件)
2. [環境設定](#環境設定)
3. [ビルドとデプロイ](#ビルドとデプロイ)
4. [デプロイ後の設定](#デプロイ後の設定)
5. [トラブルシューティング](#トラブルシューティング)

## 前提条件

### 必要なツール

以下のツールがインストールされている必要があります：

1. **Java 21**
   ```bash
   java -version
   # java version "21" 以上が表示されること
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   # Apache Maven 3.8.0 以上が表示されること
   ```

3. **AWS CLI**
   ```bash
   aws --version
   # aws-cli/2.x.x 以上が表示されること
   ```

4. **AWS CDK CLI**
   ```bash
   npm install -g aws-cdk
   cdk --version
   # 2.x.x 以上が表示されること
   ```

5. **Node.js** (CDK CLIのため)
   ```bash
   node --version
   # v18.x.x 以上が表示されること
   ```

### AWS認証情報の設定

AWS CLIの認証情報を設定します：

```bash
aws configure
```

以下の情報を入力します：
- AWS Access Key ID
- AWS Secret Access Key
- Default region name (例: ap-northeast-1)
- Default output format (json)

## 環境設定

### 1. 環境変数の設定

デプロイ前に以下の環境変数を設定します：

```bash
# JWT秘密鍵（本番環境では強力なランダム文字列を使用）
export JWT_SECRET="your-secure-random-jwt-secret-key-here"

# MCPエンドポイント
export MCP_ENDPOINT="https://your-mcp-server-endpoint"

# AWSアカウントID（オプション、aws configureで設定済みの場合は不要）
export CDK_DEFAULT_ACCOUNT="123456789012"

# AWSリージョン（オプション、aws configureで設定済みの場合は不要）
export CDK_DEFAULT_REGION="ap-northeast-1"
```

**重要**: `JWT_SECRET`は本番環境では必ず強力なランダム文字列を使用してください。

### 2. 環境変数の永続化（オプション）

環境変数を永続化する場合は、`.bashrc`または`.zshrc`に追加します：

```bash
echo 'export JWT_SECRET="your-secret-key"' >> ~/.bashrc
echo 'export MCP_ENDPOINT="https://your-mcp-endpoint"' >> ~/.bashrc
source ~/.bashrc
```

## ビルドとデプロイ

### 方法1: 自動デプロイスクリプトを使用（推奨）

最も簡単な方法は、提供されているデプロイスクリプトを使用することです：

```bash
cd cdk
chmod +x deploy.sh
./deploy.sh
```

このスクリプトは以下を自動的に実行します：
1. Lambda関数のビルド
2. CDKプロジェクトのビルド
3. CloudFormationテンプレートの生成
4. AWSへのデプロイ

### 方法2: 手動デプロイ

手動でステップごとにデプロイする場合：

#### Step 1: Lambda関数のビルド

```bash
# プロジェクトルートディレクトリで実行
mvn clean package
```

これにより、`target/health-chat-advisor-1.0.0-SNAPSHOT.jar`が生成されます。

#### Step 2: CDKプロジェクトのビルド

```bash
cd cdk
mvn compile
```

#### Step 3: CDKブートストラップ（初回のみ）

初めてCDKを使用する場合、AWSアカウントとリージョンをブートストラップします：

```bash
cdk bootstrap
```

#### Step 4: CloudFormationテンプレートの確認

デプロイ前に、生成されるCloudFormationテンプレートを確認できます：

```bash
cdk synth
```

生成されたテンプレートは`cdk.out/`ディレクトリに保存されます。

#### Step 5: デプロイ

```bash
cdk deploy
```

確認プロンプトが表示されたら、`y`を入力して続行します。

自動承認する場合：

```bash
cdk deploy --require-approval never
```

## デプロイ後の設定

### 1. API Gatewayエンドポイントの確認

デプロイが完了すると、以下のような出力が表示されます：

```
Outputs:
HealthChatAdvisorStack.HealthChatApiEndpoint = https://xxxxxxxxxx.execute-api.ap-northeast-1.amazonaws.com/prod/
```

このURLをメモしておきます。

### 2. S3バケット名の確認

```bash
aws s3 ls | grep health-chat-data
```

### 3. Lambda関数の確認

```bash
aws lambda list-functions --query 'Functions[?starts_with(FunctionName, `health-chat`)].FunctionName'
```

以下の3つの関数が表示されるはずです：
- health-chat-auth
- health-chat-chat
- health-chat-analysis

### 4. 初期データのアップロード

食品データベースをS3にアップロードします：

```bash
# S3バケット名を環境変数に設定
BUCKET_NAME=$(aws s3 ls | grep health-chat-data | awk '{print $3}')

# 食品データベースをアップロード
aws s3 cp src/main/resources/food-database.json s3://$BUCKET_NAME/food-database.json
```

### 5. フロントエンドの設定

SpringBootアプリケーション（タスク13で実装予定）の設定ファイルに、API GatewayのエンドポイントURLを設定します。

## デプロイされるリソース

### S3バケット
- **名前**: `health-chat-data-{account-id}`
- **用途**: ユーザーデータ、健康記録、栄養情報、心理状態、短歌の保存
- **ライフサイクルポリシー**: 90日後にInfrequent Accessストレージクラスに移行

### Lambda関数

1. **health-chat-auth**
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.AuthHandler::handleRequest`

2. **health-chat-chat**
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.ChatHandler::handleRequest`

3. **health-chat-analysis**
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.AnalysisHandler::handleRequest`

### API Gateway

- **名前**: Health Chat API
- **ステージ**: prod
- **エンドポイント**:
  - `POST /auth/login` - ログイン
  - `POST /auth/logout` - ログアウト
  - `POST /auth/validate` - トークン検証
  - `POST /chat` - メッセージ送信
  - `GET /analysis/nutrition` - 栄養情報取得
  - `GET /analysis/mental` - 心理状態取得
  - `GET /analysis/tanka` - 短歌取得
  - `GET /analysis/graph` - グラフデータ取得

### IAM権限

各Lambda関数には以下の権限が付与されます：
- S3バケットへの読み書き権限
- CloudWatch Logsへのログ出力権限

## コスト見積もり

### 月間コスト（想定）

以下は、1日100リクエストを想定した月間コストの概算です：

- **Lambda**: 約$0.20/月
  - 3,000リクエスト/月 × 3関数
  - 平均実行時間: 1秒
  
- **API Gateway**: 約$0.01/月
  - 3,000リクエスト/月

- **S3**: 約$0.50/月
  - ストレージ: 1GB
  - リクエスト: 3,000回/月

- **CloudWatch Logs**: 約$0.10/月
  - ログデータ: 100MB/月

**合計**: 約$0.81/月

実際のコストは使用量によって変動します。

## スタックの更新

コードを変更した後、スタックを更新する場合：

```bash
# Lambda関数を再ビルド
mvn clean package

# CDKで更新をデプロイ
cd cdk
cdk deploy
```

## スタックの削除

リソースを完全に削除する場合：

```bash
cd cdk
cdk destroy
```

**注意**: S3バケットにデータが残っている場合、削除に失敗する可能性があります。その場合は、先にバケットを空にしてください：

```bash
BUCKET_NAME=$(aws s3 ls | grep health-chat-data | awk '{print $3}')
aws s3 rm s3://$BUCKET_NAME --recursive
cdk destroy
```

## トラブルシューティング

### デプロイエラー

#### エラー: "Unable to resolve AWS account to use"

**原因**: AWS認証情報が設定されていない

**解決方法**:
```bash
aws configure
```

#### エラー: "Lambda function JAR not found"

**原因**: Lambda関数のJARファイルがビルドされていない

**解決方法**:
```bash
cd ..
mvn clean package
cd cdk
cdk deploy
```

#### エラー: "Stack already exists"

**原因**: 同じ名前のスタックが既に存在する

**解決方法**:
```bash
cdk deploy --force
```

### Lambda実行エラー

#### エラー: "Task timed out after 30.00 seconds"

**原因**: Lambda関数の実行時間が30秒を超えた

**解決方法**: `HealthChatStack.java`でタイムアウトを延長：
```java
.timeout(Duration.seconds(60))
```

#### エラー: "Access Denied" (S3)

**原因**: Lambda関数にS3へのアクセス権限がない

**解決方法**: CDKスタックを再デプロイして権限を再設定：
```bash
cdk deploy --force
```

### CloudWatch Logsの確認

Lambda関数のログを確認する方法：

```bash
# 認証Lambda
aws logs tail /aws/lambda/health-chat-auth --follow

# 対話Lambda
aws logs tail /aws/lambda/health-chat-chat --follow

# 分析Lambda
aws logs tail /aws/lambda/health-chat-analysis --follow
```

## セキュリティのベストプラクティス

1. **JWT_SECRET**: 本番環境では必ず強力なランダム文字列を使用
2. **IAM権限**: 最小権限の原則に従い、必要な権限のみを付与
3. **API Gateway**: 本番環境ではカスタムドメインとSSL証明書を使用
4. **S3バケット**: バケットポリシーで適切なアクセス制御を設定
5. **環境変数**: AWS Secrets Managerを使用して機密情報を管理（推奨）

## 参考資料

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [AWS Lambda Java Documentation](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html)
- [API Gateway Documentation](https://docs.aws.amazon.com/apigateway/)
- [S3 Documentation](https://docs.aws.amazon.com/s3/)
