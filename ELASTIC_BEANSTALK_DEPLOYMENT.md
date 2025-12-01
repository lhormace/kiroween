# Elastic Beanstalk デプロイメントガイド

このガイドでは、Health Chat AdvisorアプリケーションをAWS Elastic Beanstalkにデプロイする方法を説明します。

## 📋 前提条件

### 1. AWS CLI のインストール確認
```bash
aws --version
```

既にインストール済みです ✅

### 2. EB CLI のインストール
```bash
pip install awsebcli --upgrade --user
```

インストール確認：
```bash
eb --version
```

### 3. AWS認証情報の確認
```bash
aws sts get-caller-identity
```

既に設定済みです ✅

## 🚀 デプロイ手順

### 方法1: 自動デプロイスクリプト（推奨）

```bash
./deploy-eb.sh
```

このスクリプトは以下を自動的に実行します：
1. 前提条件のチェック
2. アプリケーションのビルド
3. Elastic Beanstalkの初期化
4. 環境の作成（初回のみ）
5. アプリケーションのデプロイ

### 方法2: 手動デプロイ

#### ステップ1: アプリケーションのビルド
```bash
mvn clean package -DskipTests
```

#### ステップ2: EB CLIの初期化（初回のみ）
```bash
eb init health-chat-advisor --platform "Corretto 17" --region ap-northeast-1
```

#### ステップ3: 環境の作成（初回のみ）
```bash
# 単一インスタンス構成（Auto Scaling無効）
eb create health-chat-env \
  --single \
  --instance-type t3.small \
  --envvars JWT_SECRET=$(openssl rand -base64 32)
```

#### ステップ4: デプロイ
```bash
eb deploy health-chat-env
```

## 🔧 環境変数の設定

### JWT秘密鍵の設定
```bash
eb setenv JWT_SECRET=$(openssl rand -base64 32)
```

### S3バケット名の設定（オプション）
```bash
eb setenv S3_BUCKET_NAME=your-bucket-name
```

## 📊 デプロイ後の確認

### アプリケーションを開く
```bash
eb open health-chat-env
```

### ログの確認
```bash
eb logs health-chat-env
```

### ステータスの確認
```bash
eb status health-chat-env
```

### 環境変数の確認
```bash
eb printenv health-chat-env
```

## 🔄 更新デプロイ

コードを変更した後、再デプロイするには：

```bash
mvn clean package -DskipTests
eb deploy health-chat-env
```

## 🛠️ トラブルシューティング

### ログの確認
```bash
# 最新のログを表示
eb logs health-chat-env

# ログをファイルに保存
eb logs health-chat-env --all > eb-logs.txt
```

### 環境の再起動
```bash
eb restart health-chat-env
```

### 環境の削除（必要な場合）
```bash
eb terminate health-chat-env
```

## 💰 コスト管理

### 使用リソース（単一インスタンス構成）
- **EC2インスタンス**: t3.small（約$0.02/時間）
- **Elastic IP**: 無料（インスタンスに関連付けられている間）
- **S3ストレージ**: 使用量に応じて

**注**: 単一インスタンス構成のため、Application Load Balancerは使用しません（コスト削減）

### 環境の停止（コスト削減）
開発中で一時的に使用しない場合：
```bash
eb terminate health-chat-env
```

再度必要になったら：
```bash
eb create health-chat-env --instance-type t3.small
```

## 🔐 セキュリティ設定

### HTTPS の有効化（推奨）
1. AWS Certificate Manager (ACM) で証明書を取得
2. Elastic Beanstalkコンソールで証明書を設定
3. HTTPSリスナーを追加

### セキュリティグループの設定
Elastic Beanstalkコンソールから：
1. 環境を選択
2. Configuration → Instances → Edit
3. セキュリティグループを設定

## 📝 設定ファイル

### .ebextensions/environment.config
環境変数とインスタンス設定

### Procfile
アプリケーションの起動コマンド

### application-production.properties
本番環境用のSpring Boot設定

## 🌐 カスタムドメインの設定

1. Route 53でドメインを管理
2. Elastic Beanstalk環境のURLをCNAMEレコードとして追加
3. ACMで証明書を取得してHTTPSを有効化

## 📞 サポート

問題が発生した場合：
1. ログを確認: `eb logs health-chat-env`
2. ステータスを確認: `eb status health-chat-env`
3. CloudWatchログを確認
4. Elastic Beanstalkコンソールでヘルスステータスを確認

## 🎯 次のステップ

1. ✅ EB CLIをインストール
2. ✅ デプロイスクリプトを実行
3. ✅ アプリケーションの動作確認
4. ⬜ HTTPSの設定
5. ⬜ カスタムドメインの設定
6. ⬜ モニタリングとアラートの設定
