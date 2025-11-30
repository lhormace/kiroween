# Health Chat Advisor（健康管理チャットアドバイザー）

健康管理チャットアドバイザーは、ユーザーが気軽に健康情報を入力し、AIとの対話を通じて健康管理を行うシステムです。

## 概要

このシステムは、SNSに投稿するような感覚で体重・体脂肪・食事・運動などの情報を入力でき、システムが自動的に栄養素を推定し、パーソナライズされたアドバイスを提供します。

### 主な機能

- **気軽な健康情報入力**: 140文字以内の短いメッセージで対話
- **自動栄養素推定**: 食事内容から栄養素を自動計算
- **科学的根拠に基づくアドバイス**: MCP経由で最新の研究情報を参照
- **心理状態分析**: 対話内容から心理状態を分析し、適切なトーンでアドバイス
- **短歌による記録**: 日々の健康記録を短歌として保存
- **グラフ表示**: 体重・体脂肪率の推移を視覚化

## 技術スタック

- **バックエンド**: Java 21, SpringBoot
- **インフラ**: AWS Lambda, API Gateway, S3
- **IaC**: AWS CDK (Java)
- **テスト**: JUnit 5, jqwik (Property-Based Testing)
- **外部統合**: MCP (Model Context Protocol)

## プロジェクト構造

```
.
├── src/                          # メインアプリケーション
│   ├── main/java/com/health/chat/
│   │   ├── lambda/              # Lambda関数ハンドラー
│   │   ├── model/               # データモデル
│   │   ├── repository/          # データアクセス層
│   │   └── service/             # ビジネスロジック
│   └── test/                    # テストコード
├── cdk/                         # AWS CDKインフラストラクチャ
│   ├── src/main/java/           # CDKスタック定義
│   └── README.md                # CDKデプロイガイド
├── pom.xml                      # Mavenプロジェクト設定
└── DEPLOYMENT.md                # デプロイメントガイド
```

## セットアップ

### 前提条件

- Java 21
- Maven 3.8+
- AWS CLI
- AWS CDK CLI
- Node.js 18+

### ビルド

```bash
mvn clean package
```

### テスト実行

```bash
mvn test
```

## デプロイ

詳細なデプロイ手順は [DEPLOYMENT.md](DEPLOYMENT.md) を参照してください。

### クイックスタート

```bash
# 前提条件のチェック
cd cdk
chmod +x check-prerequisites.sh
./check-prerequisites.sh

# デプロイ
chmod +x deploy.sh
./deploy.sh
```

## ドキュメント

- [デプロイメントガイド](DEPLOYMENT.md) - AWSへのデプロイ手順
- [CDK README](cdk/README.md) - CDKインフラストラクチャの詳細
- [インフラストラクチャ詳細](cdk/INFRASTRUCTURE.md) - AWSリソースの詳細説明
- [プロジェクトセットアップ](PROJECT_SETUP.md) - 開発環境のセットアップ

## 仕様書

仕様書は `.kiro/specs/health-chat-advisor/` ディレクトリに格納されています：

- [requirements.md](.kiro/specs/health-chat-advisor/requirements.md) - 要件定義
- [design.md](.kiro/specs/health-chat-advisor/design.md) - 設計書
- [tasks.md](.kiro/specs/health-chat-advisor/tasks.md) - 実装タスク

## ライセンス

このプロジェクトは個人利用を目的としています。
