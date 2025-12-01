# Elastic Beanstalk EC2自動停止 - クイックスタート

## 📋 概要

Elastic Beanstalk環境のEC2インスタンスを毎日17:00（JST）に自動停止します。

## 🚀 セットアップ（3ステップ）

### ステップ1: Elastic Beanstalkにデプロイ

```bash
./deploy-eb.sh
```

### ステップ2: EC2自動停止スタックをデプロイ

```bash
cd cdk
cdk deploy Ec2SchedulerStack
```

### ステップ3: 動作確認

```bash
# Lambda関数を手動実行してテスト
aws lambda invoke \
  --function-name ec2-auto-stop \
  --payload '{}' \
  response.json

cat response.json
```

## ✅ 完了！

これで毎日17:00（JST）に自動的にEC2インスタンスが停止します。

## 🔧 設定内容

- **停止時刻**: 毎日17:00 JST（08:00 UTC）
- **対象環境**: `health-chat-env`（Elastic Beanstalk環境名）
- **検出方法**: `elasticbeanstalk:environment-name` タグで自動検出

## 📝 環境名を変更する場合

別のElastic Beanstalk環境を使用している場合：

1. `cdk/src/main/java/com/health/chat/cdk/Ec2SchedulerStack.java` を編集
2. 環境変数を変更：

```java
.environment(Map.of(
    "INSTANCE_IDS", "",
    "EB_ENVIRONMENT_NAME", "your-environment-name"  // ここを変更
))
```

3. 再デプロイ：

```bash
cd cdk
cdk deploy Ec2SchedulerStack
```

## 🔍 トラブルシューティング

### インスタンスが停止しない場合

```bash
# 1. Elastic Beanstalk環境を確認
eb list

# 2. Lambda関数のログを確認
aws logs tail /aws/lambda/ec2-auto-stop --follow

# 3. EC2インスタンスを確認
aws ec2 describe-instances \
  --filters "Name=tag:elasticbeanstalk:environment-name,Values=health-chat-env" \
  --query "Reservations[].Instances[].{ID:InstanceId,State:State.Name}"
```

### 手動でインスタンスを起動する

```bash
# Elastic Beanstalk環境のインスタンスIDを取得
INSTANCE_ID=$(aws ec2 describe-instances \
  --filters "Name=tag:elasticbeanstalk:environment-name,Values=health-chat-env" \
  --query "Reservations[0].Instances[0].InstanceId" \
  --output text)

# インスタンスを起動
aws ec2 start-instances --instance-ids $INSTANCE_ID

# 状態確認
aws ec2 describe-instances \
  --instance-ids $INSTANCE_ID \
  --query "Reservations[0].Instances[0].State.Name"
```

## ⏰ 停止時刻を変更する

`cdk/src/main/java/com/health/chat/cdk/Ec2SchedulerStack.java` を編集：

```java
.schedule(Schedule.cron(
    software.amazon.awscdk.services.events.CronOptions.builder()
        .minute("0")
        .hour("9")  // 18:00 JST = 09:00 UTC
        .build()
))
```

時刻変換表：
- 16:00 JST = 07:00 UTC
- 17:00 JST = 08:00 UTC
- 18:00 JST = 09:00 UTC
- 19:00 JST = 10:00 UTC

## 💰 コスト削減効果

**例**: t3.small を毎日16時間停止

- 停止前: 24時間 × 30日 × $0.02 = **$14.40/月**
- 停止後: 8時間 × 30日 × $0.02 = **$4.80/月**
- **削減額: $9.60/月（67%削減）**

## 🗑️ 削除方法

自動停止が不要になった場合：

```bash
cd cdk
cdk destroy Ec2SchedulerStack
```

## 📚 詳細ドキュメント

より詳しい情報は以下を参照：
- [cdk/EC2_AUTO_STOP_SETUP.md](cdk/EC2_AUTO_STOP_SETUP.md) - 完全なセットアップガイド
- [ELASTIC_BEANSTALK_DEPLOYMENT.md](ELASTIC_BEANSTALK_DEPLOYMENT.md) - Elastic Beanstalkデプロイガイド

## ⚠️ 注意事項

1. **Elastic Beanstalkの自動スケーリング**
   - Auto Scalingが有効な場合、停止したインスタンスが自動的に再起動される可能性があります
   - 開発環境では単一インスタンス構成を推奨

2. **本番環境での使用**
   - 本番環境では24時間稼働が必要な場合が多いため、開発/テスト環境での使用を推奨

3. **データの永続化**
   - インスタンス停止前にデータがS3に保存されていることを確認してください
