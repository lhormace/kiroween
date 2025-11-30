# 健康管理チャットアドバイザー - Webアプリケーション

## 概要

このSpring Boot Webアプリケーションは、健康管理チャットアドバイザーのフロントエンドインターフェースを提供します。

## 機能

- **ログインページ**: ユーザー認証
- **チャットインターフェース**: 健康情報の入力とAIとの対話
- **セッション管理**: 24時間のセッション有効期限
- **API Gateway統合**: バックエンドLambda関数との通信

## 技術スタック

- Spring Boot 3.2.0
- Spring Security
- Thymeleaf (テンプレートエンジン)
- Java 21

## 起動方法

### 開発環境

```bash
# API Gateway URLを環境変数として設定
export API_GATEWAY_URL=http://localhost:8080

# アプリケーションを起動
mvn spring-boot:run
```

アプリケーションは http://localhost:8081 で起動します。

### 本番環境

```bash
# API Gateway URLを環境変数として設定
export API_GATEWAY_URL=https://your-api-gateway-url.amazonaws.com

# プロファイルを指定して起動
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

## 設定

### application.properties

- `server.port`: Webサーバーのポート (デフォルト: 8081)
- `api.gateway.url`: API GatewayのURL
- `server.servlet.session.timeout`: セッションタイムアウト (デフォルト: 24時間)

### セキュリティ

- Spring Securityを使用してセッション管理
- CSRFは無効化（API Gateway経由の通信のため）
- ログインページ以外は認証が必要

## エンドポイント

- `GET /`: ルートパス（ログイン済みの場合はチャットにリダイレクト）
- `GET /login`: ログインページ
- `POST /login`: ログイン処理
- `POST /logout`: ログアウト処理
- `GET /chat`: チャットページ
- `POST /api/chat`: メッセージ送信API

## 開発

### ディレクトリ構造

```
src/main/java/com/health/chat/
├── HealthChatApplication.java      # メインアプリケーションクラス
├── config/
│   └── SecurityConfig.java         # Spring Security設定
├── web/
│   ├── AuthController.java         # 認証コントローラー
│   └── ChatController.java         # チャットコントローラー
└── service/
    └── ApiGatewayClient.java       # API Gateway通信クライアント

src/main/resources/
├── templates/
│   ├── login.html                  # ログインページ
│   └── chat.html                   # チャットページ
├── application.properties          # 開発環境設定
└── application-production.properties # 本番環境設定
```

## トラブルシューティング

### API Gatewayに接続できない

- `API_GATEWAY_URL`環境変数が正しく設定されているか確認
- API Gatewayが起動しているか確認
- ネットワーク接続を確認

### セッションが切れる

- セッションタイムアウトは24時間に設定されています
- ブラウザのCookieが有効になっているか確認

## ライセンス

このプロジェクトは健康管理チャットアドバイザーシステムの一部です。
