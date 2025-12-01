# EC2自動停止設定ガイド

## 概要

毎日17:00（JST）にEC2インスタンスを自動停止するスケジューラーです。

## アーキテクチャ

```
EventBridge (Cron: 17:00 JST)
    ↓
Lambda Function (Python)
    ↓
EC2 API (StopInstances)
```

## デプロイ方法

### 1. CDKスタックのデプロイ

```bash
cd cdk
cdk deploy Ec2SchedulerStack
```

### 2. 対象インスタンスの設定

3つの方法から選択できます：

#### 方法A: Elastic Beanstalk環境名を指定（推奨）

Elastic Beanstalkでデプロイしている場合、環境名を指定するだけで自動的にインスタンスを検出します。

**デフォルト設定**:
- 環境名: `health-chat-env`（既に設定済み）

**別の環境名を使用する場合**:

1. `cdk/src/main/java/com/health/chat/cdk/Ec2SchedulerStack.java` を編集
2. 環境変数 `EB_ENVIRONMENT_NAME` を変更：

```java
.environment(Map.of(
    "INSTANCE_IDS", "",
    "EB_ENVIRONMENT_NAME", "your-environment-name"
))
```

3. 再デプロイ：

```bash
cd cdk
cdk deploy Ec2SchedulerStack
```

#### 方法B: EC2インスタンスにタグを追加

Elastic Beanstalk以外のEC2インスタンスを停止する場合：

1. EC2コンソールを開く
2. 対象のインスタンスを選択
3. 「タグ」タブ → 「タグの管理」
4. キー: `AutoStop`、値: `true` を追加

AWS CLIの場合：
```bash
aws ec2 create-tags \
  --resources i-1234567890abcdef0 \
  --tags Key=AutoStop,Value=true
```

#### 方法C: 特定のインスタンスIDを指定

特定のインスタンスIDを直接指定する場合：

1. `cdk/src/main/java/com/health/chat/cdk/Ec2SchedulerStack.java` を編集
2. 環境変数 `INSTANCE_IDS` を設定：

```java
.environment(Map.of(
    "INSTANCE_IDS", "i-1234567890abcdef0,i-0987654321fedcba0",
    "EB_ENVIRONMENT_NAME", ""
))
```

3. 再デプロイ：

```bash
cd cdk
cdk deploy Ec2SchedulerStack
```

## スケジュール変更

停止時刻を変更したい場合は、`Ec2SchedulerStack.java` の以下の部分を編集してください：

```java
.schedule(Schedule.cron(
    software.amazon.awscdk.services.events.CronOptions.builder()
        .minute("0")
        .hour("8")  // 17:00 JST = 08:00 UTC
        .build()
))
```

### 時刻変換表（JST → UTC）

| JST | UTC |
|-----|-----|
| 16:00 | 07:00 |
| 17:00 | 08:00 |
| 18:00 | 09:00 |
| 19:00 | 10:00 |
| 20:00 | 11:00 |

## 動作確認

### 1. Lambda関数の手動実行

```bash
# Lambda関数名を確認
aws lambda list-functions --query "Functions[?FunctionName=='ec2-auto-stop'].FunctionName"

# 手動実行
aws lambda invoke \
  --function-name ec2-auto-stop \
  --payload '{}' \
  response.json

# 結果確認
cat response.json
```

### 2. CloudWatch Logsで確認

```bash
# ロググループを確認
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/ec2-auto-stop

# 最新のログストリームを確認
aws logs tail /aws/lambda/ec2-auto-stop --follow
```

### 3. EventBridgeルールの確認

```bash
# ルールの確認
aws events describe-rule --name stop-ec2-daily-17-00-jst

# ターゲットの確認
aws events list-targets-by-rule --rule stop-ec2-daily-17-00-jst
```

## トラブルシューティング

### インスタンスが停止しない場合

1. **Elastic Beanstalk環境の確認**
   ```bash
   # 環境名の確認
   aws elasticbeanstalk describe-environments \
     --query "Environments[].{Name:EnvironmentName,Status:Status}"
   
   # 環境のEC2インスタンスを確認
   aws ec2 describe-instances \
     --filters "Name=tag:elasticbeanstalk:environment-name,Values=health-chat-env" \
     --query "Reservations[].Instances[].{ID:InstanceId,State:State.Name,Name:Tags[?Key=='Name'].Value|[0]}"
   ```

2. **Lambda関数のログ確認**
   ```bash
   aws logs tail /aws/lambda/ec2-auto-stop --follow
   ```

3. **手動でLambda関数をテスト**
   ```bash
   aws lambda invoke \
     --function-name ec2-auto-stop \
     --payload '{}' \
     response.json
   
   cat response.json
   ```

4. **IAM権限の確認**
   Lambda関数のロールに以下の権限があることを確認：
   - `ec2:DescribeInstances`
   - `ec2:StopInstances`

5. **タグの確認（方法Bを使用している場合）**
   ```bash
   aws ec2 describe-instances \
     --filters "Name=tag:AutoStop,Values=true" \
     --query "Reservations[].Instances[].{ID:InstanceId,State:State.Name,Tags:Tags}"
   ```

### 手動でインスタンスを起動する方法

```bash
# インスタンスを起動
aws ec2 start-instances --instance-ids i-1234567890abcdef0

# 状態確認
aws ec2 describe-instances \
  --instance-ids i-1234567890abcdef0 \
  --query "Reservations[].Instances[].State.Name"
```

## 自動起動の追加（オプション）

毎朝9:00に自動起動したい場合は、同様のスタックを作成できます：

```java
// Ec2SchedulerStack.java に追加
Function startEc2Function = createStartEc2Function();

Rule startRule = Rule.Builder.create(this, "StartEc2Rule")
        .ruleName("start-ec2-daily-09-00-jst")
        .description("Start EC2 instances at 09:00 JST every day")
        .schedule(Schedule.cron(
            software.amazon.awscdk.services.events.CronOptions.builder()
                .minute("0")
                .hour("0")  // 09:00 JST = 00:00 UTC
                .build()
        ))
        .build();

startRule.addTarget(LambdaFunction.Builder.create(startEc2Function).build());
```

## コスト削減効果

### 計算例

- インスタンスタイプ: t3.medium
- 時間単価: $0.0416/時間
- 停止時間: 17:00-09:00（16時間/日）

**月間コスト削減**:
- 停止前: 24時間 × 30日 × $0.0416 = $29.95/月
- 停止後: 8時間 × 30日 × $0.0416 = $9.98/月
- **削減額: $19.97/月（約67%削減）**

## 削除方法

スケジューラーが不要になった場合：

```bash
cd cdk
cdk destroy Ec2SchedulerStack
```

## 参考リンク

- [AWS EventBridge Cron式](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-create-rule-schedule.html)
- [EC2インスタンスの停止と起動](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/Stop_Start.html)
- [Lambda関数のモニタリング](https://docs.aws.amazon.com/lambda/latest/dg/monitoring-functions.html)
