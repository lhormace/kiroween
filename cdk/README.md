# Health Chat Advisor - AWS CDK Infrastructure

このディレクトリには、Health Chat Advisorシステムのインフラストラクチャをコードとして定義するAWS CDKプロジェクトが含まれています。

## 概要

このCDKスタックは以下のAWSリソースをデプロイします：

### S3バケット
- **health-chat-data-{account-id}**: ユーザーデータ、健康記録、栄養情報、心理状態、短歌を保存
- ライフサイクルポリシー: 90日後にInfrequent Accessストレージクラスに移行（コスト最適化）

### Lambda関数
1. **health-chat-auth**: 認証処理（ログイン、ログアウト、トークン検証）
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.AuthHandler::handleRequest`

2. **health-chat-chat**: 対話処理（メッセージ解析、アドバイス生成）
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.ChatHandler::handleRequest`

3. **health-chat-analysis**: 分析処理（栄養推定、心理状態分析、短歌生成）
   - Runtime: Java 21
   - Memory: 512MB
   - Timeout: 30秒
   - Handler: `com.health.chat.lambda.AnalysisHandler::handleRequest`

### API Gateway
- **Health Chat API**: RESTful APIエンドポイント
- エンドポイント:
  - `POST /auth/login` - ログイン
  - `POST /auth/logout` - ログアウト
  - `POST /auth/validate` - トークン検証
  - `POST /chat` - メッセージ送信
  - `GET /analysis/nutrition` - 栄養情報取得
  - `GET /analysis/mental` - 心理状態取得
  - `GET /analysis/tanka` - 短歌取得
  - `GET /analysis/graph` - グラフデータ取得

### IAM権限
- 各Lambda関数にS3バケットへの読み書き権限を付与
- CloudWatch Logsへのログ出力権限を自動付与

## 前提条件

1. AWS CLIがインストールされ、設定されていること
2. AWS CDK CLIがインストールされていること
   ```bash
   npm install -g aws-cdk
   ```
3. Java 21がインストールされていること
4. Mavenがインストールされていること

## デプロイ手順

### 1. Lambda関数のビルド

まず、親ディレクトリでLambda関数をビルドします：

```bash
cd ..
mvn clean package
```

これにより、`target/health-chat-advisor-1.0.0-SNAPSHOT.jar`が生成されます。

### 2. CDKプロジェクトのビルド

CDKディレクトリに戻り、プロジェクトをビルドします：

```bash
cd cdk
mvn compile
```

### 3. CDKのブートストラップ（初回のみ）

初めてCDKを使用する場合、AWSアカウントとリージョンをブートストラップする必要があります：

```bash
cdk bootstrap
```

### 4. CloudFormationテンプレートの生成

デプロイ前に、生成されるCloudFormationテンプレートを確認できます：

```bash
cdk synth
```

### 5. デプロイ

スタックをデプロイします：

```bash
cdk deploy
```

確認プロンプトが表示されたら、`y`を入力して続行します。

## 環境変数

デプロイ前に以下の環境変数を設定することを推奨します：

```bash
export JWT_SECRET="your-secure-jwt-secret-key"
export MCP_ENDPOINT="https://your-mcp-server-endpoint"
export CDK_DEFAULT_ACCOUNT="your-aws-account-id"
export CDK_DEFAULT_REGION="ap-northeast-1"
```

## デプロイ後

デプロイが完了すると、以下の情報が出力されます：

- API GatewayのエンドポイントURL
- S3バケット名
- Lambda関数のARN

これらの情報を使用して、フロントエンドアプリケーションを設定します。

## スタックの削除

リソースを削除する場合：

```bash
cdk destroy
```

**注意**: S3バケットにデータが残っている場合、削除に失敗する可能性があります。その場合は、先にバケットを空にしてください。

## コスト最適化

このインフラストラクチャは以下の方法でコストを最適化しています：

1. **サーバーレスアーキテクチャ**: Lambda関数は実行時のみ課金
2. **S3ライフサイクルポリシー**: 90日後に低コストストレージに移行
3. **CloudWatch Logsの保持期間**: 30日に設定
4. **適切なLambdaメモリサイズ**: 512MBで最適化

## トラブルシューティング

### デプロイエラー

- **Lambda関数のJARファイルが見つからない**: 親ディレクトリで`mvn package`を実行してください
- **権限エラー**: AWS CLIの認証情報を確認してください
- **リージョンエラー**: `CDK_DEFAULT_REGION`環境変数を設定してください

### Lambda実行エラー

- CloudWatch Logsでエラーログを確認してください
- 環境変数が正しく設定されているか確認してください

## 参考資料

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [AWS Lambda Java Documentation](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html)
- [API Gateway Documentation](https://docs.aws.amazon.com/apigateway/)
