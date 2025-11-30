# Health Chat Advisor - インフラストラクチャ詳細

## アーキテクチャ図

```
┌─────────────┐
│   ユーザー   │
└──────┬──────┘
       │ HTTPS
       ▼
┌─────────────────────────────────────────┐
│         API Gateway (REST API)          │
│  - /auth/login, /auth/logout           │
│  - /chat                                │
│  - /analysis/*                          │
└──────┬──────────────────────────────────┘
       │
       ├─────────────┬─────────────┬──────────────┐
       ▼             ▼             ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│  Auth    │  │  Chat    │  │ Analysis │  │   S3     │
│ Lambda   │  │ Lambda   │  │ Lambda   │  │  Bucket  │
│ (Java21) │  │ (Java21) │  │ (Java21) │  │          │
└────┬─────┘  └────┬─────┘  └────┬─────┘  └──────────┘
     │             │             │
     └─────────────┴─────────────┘
                   │
                   ▼
            ┌──────────────┐
            │  CloudWatch  │
            │     Logs     │
            └──────────────┘
```

## リソース一覧

### 1. S3バケット

**リソース名**: `HealthDataBucket`
**物理名**: `health-chat-data-{account-id}`

**ディレクトリ構造**:
```
health-chat-data-{account-id}/
├── food-database.json
└── users/
    └── {userId}/
        ├── profile.json
        ├── health/
        │   └── {year}/
        │       └── {month}/
        │           └── {day}.json
        ├── nutrition/
        │   └── {year}/
        │       └── {month}/
        │           └── {day}.json
        ├── mental/
        │   └── {year}/
        │       └── {month}/
        │           └── {day}.json
        └── tanka/
            └── {year}/
                └── {month}/
                    └── {day}.json
```

**ライフサイクルポリシー**:
- 90日後にSTANDARD_IAストレージクラスに移行

**アクセス権限**:
- 3つのLambda関数からの読み書きアクセス

### 2. Lambda関数

#### 2.1 認証Lambda (AuthLambda)

**関数名**: `health-chat-auth`
**ランタイム**: Java 21
**ハンドラー**: `com.health.chat.lambda.AuthHandler::handleRequest`
**メモリ**: 512MB
**タイムアウト**: 30秒
**ログ保持期間**: 30日

**環境変数**:
- `BUCKET_NAME`: S3バケット名
- `JWT_SECRET`: JWT署名用の秘密鍵

**IAM権限**:
- S3バケットへの読み書き
- CloudWatch Logsへの書き込み

**処理内容**:
- ユーザー認証（ログイン）
- トークン生成
- トークン検証
- ログアウト処理

#### 2.2 対話Lambda (ChatLambda)

**関数名**: `health-chat-chat`
**ランタイム**: Java 21
**ハンドラー**: `com.health.chat.lambda.ChatHandler::handleRequest`
**メモリ**: 512MB
**タイムアウト**: 30秒
**ログ保持期間**: 30日

**環境変数**:
- `BUCKET_NAME`: S3バケット名
- `JWT_SECRET`: JWT検証用の秘密鍵
- `MCP_ENDPOINT`: MCPサーバーのエンドポイント

**IAM権限**:
- S3バケットへの読み書き
- CloudWatch Logsへの書き込み

**処理内容**:
- メッセージ解析
- 健康データ抽出
- MCP経由での研究情報取得
- アドバイス生成

#### 2.3 分析Lambda (AnalysisLambda)

**関数名**: `health-chat-analysis`
**ランタイム**: Java 21
**ハンドラー**: `com.health.chat.lambda.AnalysisHandler::handleRequest`
**メモリ**: 512MB
**タイムアウト**: 30秒
**ログ保持期間**: 30日

**環境変数**:
- `BUCKET_NAME`: S3バケット名
- `MCP_ENDPOINT`: MCPサーバーのエンドポイント

**IAM権限**:
- S3バケットへの読み書き
- CloudWatch Logsへの書き込み

**処理内容**:
- 栄養素推定
- 心理状態分析
- 短歌生成
- グラフデータ取得

### 3. API Gateway

**API名**: `Health Chat API`
**ステージ**: `prod`
**プロトコル**: REST API

**エンドポイント一覧**:

| メソッド | パス | Lambda関数 | 認証 | 説明 |
|---------|------|-----------|------|------|
| POST | /auth/login | AuthLambda | なし | ログイン |
| POST | /auth/logout | AuthLambda | なし | ログアウト |
| POST | /auth/validate | AuthLambda | なし | トークン検証 |
| POST | /chat | ChatLambda | カスタム | メッセージ送信 |
| GET | /analysis/nutrition | AnalysisLambda | なし | 栄養情報取得 |
| GET | /analysis/mental | AnalysisLambda | なし | 心理状態取得 |
| GET | /analysis/tanka | AnalysisLambda | なし | 短歌取得 |
| GET | /analysis/graph | AnalysisLambda | なし | グラフデータ取得 |

**CORS設定**:
- すべてのオリジンを許可
- すべてのメソッドを許可

**ロギング**:
- ログレベル: INFO
- データトレース: 有効

### 4. IAM ロール

各Lambda関数に対して、以下の権限を持つIAMロールが自動生成されます：

**ポリシー**:
1. **S3アクセス**:
   - `s3:GetObject`
   - `s3:PutObject`
   - `s3:DeleteObject`
   - `s3:ListBucket`

2. **CloudWatch Logs**:
   - `logs:CreateLogGroup`
   - `logs:CreateLogStream`
   - `logs:PutLogEvents`

3. **Lambda基本実行ロール**:
   - AWSLambdaBasicExecutionRole

## コスト最適化戦略

### 1. Lambda関数

**最適化ポイント**:
- メモリサイズ: 512MB（必要に応じて調整可能）
- タイムアウト: 30秒（長時間実行を防止）
- コールドスタート対策: Java 21のネイティブイメージ化（将来的な改善）

**コスト削減**:
- 実行時間の最適化
- 不要な処理の削減
- 効率的なS3アクセス

### 2. S3ストレージ

**最適化ポイント**:
- ライフサイクルポリシー: 90日後にInfrequent Accessに移行
- データ圧縮: JSON形式（将来的にはGzip圧縮を検討）

**コスト削減**:
- 古いデータの自動アーカイブ
- 不要なデータの定期削除

### 3. API Gateway

**最適化ポイント**:
- キャッシング: 頻繁にアクセスされるデータをキャッシュ（将来的な改善）
- スロットリング: 過剰なリクエストを制限

**コスト削減**:
- 効率的なAPIデザイン
- バッチ処理の活用

### 4. CloudWatch Logs

**最適化ポイント**:
- ログ保持期間: 30日
- ログレベル: INFO（本番環境ではERRORのみに変更可能）

**コスト削減**:
- 不要なログの削減
- ログフィルタリング

## モニタリングとアラート

### CloudWatch メトリクス

**Lambda関数**:
- Invocations（実行回数）
- Duration（実行時間）
- Errors（エラー回数）
- Throttles（スロットリング回数）
- ConcurrentExecutions（同時実行数）

**API Gateway**:
- Count（リクエスト数）
- Latency（レイテンシ）
- 4XXError（クライアントエラー）
- 5XXError（サーバーエラー）

**S3**:
- BucketSizeBytes（バケットサイズ）
- NumberOfObjects（オブジェクト数）

### アラート設定（推奨）

以下のメトリクスに対してアラートを設定することを推奨します：

1. **Lambda エラー率 > 5%**
   - アクション: SNS通知

2. **API Gateway 5XXエラー > 10件/分**
   - アクション: SNS通知

3. **Lambda 実行時間 > 25秒**
   - アクション: SNS通知（タイムアウト警告）

## セキュリティ

### 1. 認証・認可

- JWT トークンベースの認証
- トークン有効期限: 24時間
- API Gatewayでのカスタム認証

### 2. データ暗号化

- **転送中**: HTTPS/TLS 1.2以上
- **保存時**: S3のサーバーサイド暗号化（SSE-S3）

### 3. IAM権限

- 最小権限の原則
- Lambda関数ごとに個別のIAMロール
- S3バケットへのアクセスは必要な関数のみ

### 4. ネットワーク

- API Gatewayはパブリックエンドポイント
- Lambda関数はVPC外で実行（S3アクセスのため）

### 5. ログとモニタリング

- すべてのAPI呼び出しをログ記録
- CloudWatch Logsで監査証跡を保持
- 異常なアクセスパターンの検出

## スケーラビリティ

### 自動スケーリング

**Lambda関数**:
- デフォルトの同時実行数: 1000
- 自動スケーリング: 有効
- リザーブド同時実行数: 未設定（必要に応じて設定）

**API Gateway**:
- デフォルトのスロットリング: 10,000リクエスト/秒
- バーストリミット: 5,000リクエスト

**S3**:
- 無制限のスケーラビリティ
- 自動的にリクエストを分散

### パフォーマンス最適化

1. **Lambda コールドスタート対策**:
   - プロビジョンド同時実行数の設定（オプション）
   - 初期化コードの最適化

2. **S3アクセス最適化**:
   - 適切なキープレフィックスの使用
   - バッチ処理の活用

3. **API Gateway キャッシング**:
   - GETリクエストのキャッシング（将来的な改善）
   - TTL: 300秒

## バックアップと災害復旧

### バックアップ戦略

**S3データ**:
- バージョニング: 無効（コスト削減のため）
- クロスリージョンレプリケーション: 未設定（必要に応じて設定）
- 定期的なバックアップ: AWS Backupを使用（オプション）

**Lambda関数**:
- コードはGitリポジトリで管理
- CDKでインフラをコード化

### 災害復旧

**RTO（目標復旧時間）**: 1時間
**RPO（目標復旧時点）**: 24時間

**復旧手順**:
1. 別リージョンでCDKスタックをデプロイ
2. S3データを復元（必要に応じて）
3. DNS設定を更新（Route 53使用時）

## 今後の改善案

1. **パフォーマンス**:
   - Lambda関数のネイティブイメージ化（GraalVM）
   - API Gatewayのキャッシング有効化
   - S3データの圧縮

2. **セキュリティ**:
   - AWS Secrets Managerでの秘密情報管理
   - VPC内でのLambda実行
   - WAF（Web Application Firewall）の導入

3. **可用性**:
   - マルチリージョン構成
   - Route 53でのヘルスチェック
   - DynamoDBへの移行（より高速なアクセス）

4. **コスト**:
   - Lambda関数のメモリサイズ最適化
   - S3データのGzip圧縮
   - CloudWatch Logsのフィルタリング

5. **運用**:
   - CI/CDパイプラインの構築
   - 自動テストの追加
   - カナリアデプロイメント
