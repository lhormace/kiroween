# デプロイ準備完了チェックリスト

## 📅 作成日: 2025年12月3日 23:50

## ✅ デプロイ準備状況

### 必須ファイル

- [x] `application.jar` (41MB) - ルートディレクトリに配置済み
- [x] `.ebextensions/01_setup.config` - PIDディレクトリ作成設定
- [x] `.ebextensions/environment.config` - 環境変数設定
- [x] `.ebignore` - 正しく設定済み
- [x] `.elasticbeanstalk/config.yml` - EB設定

### 動作確認

- [x] JARファイルのローカル起動テスト - **成功**
- [x] MANIFEST.MFの確認 - **正常**
- [x] Spring Profile設定 - **production**
- [x] 環境変数設定 - **完備**

### クリーンアップ

- [x] 不要なファイル削除（nginx.config.backup等）
- [x] Dockerfileを`.ebignore`で除外
- [x] Procfile削除（EBデフォルト動作に任せる）
- [x] 古い環境削除済み

## 🚀 次回セッションでの実行コマンド

### 1. 新規環境作成

```bash
eb create health-chat-env --single --instance-type t3.small --timeout 20
```

### 2. デプロイ確認（成功時）

```bash
# ステータス確認
eb status

# ヘルスチェック
curl http://health-chat-env.eba-XXXXXXXX.ap-northeast-1.elasticbeanstalk.com/actuator/health

# ログ確認（問題発生時）
eb logs --all
```

### 3. CloudWatch Logs確認（問題発生時）

```bash
aws logs tail /aws/elasticbeanstalk/health-chat-env/var/log/web.stdout.log --since 30m --region ap-northeast-1
```

## 📋 設定内容サマリー

### .ebextensions/01_setup.config
```yaml
commands:
  01_create_pids_dir:
    command: "mkdir -p /var/pids && chmod 777 /var/pids"
```

### .ebextensions/environment.config
```yaml
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: "5000"
    SPRING_PROFILES_ACTIVE: "production"
    AWS_REGION: "ap-northeast-1"
    S3_BUCKET_NAME: "health-chat-data"
    JWT_SECRET: "change-me-in-production-use-secrets-manager"
```

### .ebignore（重要部分）
```
Dockerfile
.git/
data/
cdk/
!application.jar
!.ebextensions/
```

## ⚠️ トラブルシューティング

### エラー1: "Unable to access jarfile"
**原因**: JARファイルが見つからない
**確認**: `ls -la application.jar`
**解決**: ルートに`application.jar`が存在することを確認

### エラー2: "failed to read file /var/pids/web.pid"
**原因**: PIDディレクトリが存在しない
**確認**: `.ebextensions/01_setup.config`の存在確認
**解決**: PIDディレクトリ作成設定が含まれていることを確認

### エラー3: "Invalid option specification (proxy)"
**原因**: Java プラットフォームで無効な設定
**確認**: `.ebextensions/environment.config`の内容確認
**解決**: proxy関連の設定を削除

## 🎯 成功の判断基準

### 必須条件
1. `eb status`で**Health: Green**
2. `/actuator/health`が`{"status":"UP"}`を返す
3. ログインページにアクセス可能

### 確認URL（環境作成後）
```
http://health-chat-env.eba-XXXXXXXX.ap-northeast-1.elasticbeanstalk.com/
```

## 📊 期待される結果

```
Environment details for: health-chat-env
  Application name: health-chat-advisor
  Region: ap-northeast-1
  Platform: Corretto 17 running on 64bit Amazon Linux 2
  Tier: WebServer-Standard-1.0
  Status: Ready
  Health: Green ✅
```

## 💾 バックアップ情報

### 現在のファイル状態
- `application.jar`: MD5 = a49bd7cb5008991f6e882466484d7649
- ビルド日時: 2025-12-03 23:27
- サイズ: 41MB

### Git状態
```bash
# 現在の変更を確認
git status

# 必要に応じてコミット
git add .ebextensions/ .ebignore application.jar
git commit -m "Fix EB deployment configuration"
```

## 📞 サポート情報

### 参考ドキュメント
- `DEPLOYMENT_PROGRESS_SESSION5.md` - 詳細な作業記録
- `ELASTIC_BEANSTALK_DEPLOYMENT.md` - EB デプロイガイド
- `DEPLOYMENT_STATUS.md` - 過去の状況記録

### 重要な学び
1. **PIDファイルは表面的な問題だった** - 真の原因はJARファイルの配置
2. **CloudWatch Logsが決定的** - `web.stdout.log`に真のエラー
3. **体系的な点検が重要** - 全体を見ないと問題が連鎖する

---

**準備完了日時**: 2025-12-03 23:50
**次回セッション**: 新規環境作成とデプロイ実行
**成功確率**: 高（全ての既知問題を解決済み）
