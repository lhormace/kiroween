# AWS予算管理設定ガイド

## 目的
月額$20を超えないようにコストを管理し、超過時に通知を受け取る

## 方法1: AWS Budgets（推奨）

### ステップ1: AWS Budgetsの設定

1. **AWS Billing Consoleにアクセス**
   ```
   https://console.aws.amazon.com/billing/home
   ```

2. **Budgetsを作成**
   - 左メニューから「Budgets」を選択
   - 「Create budget」をクリック

3. **予算タイプを選択**
   - 「Cost budget」を選択
   - 「Next」をクリック

4. **予算の詳細を設定**
   ```
   Budget name: Monthly-Cost-Limit
   Period: Monthly
   Budget amount: $20.00
   ```

5. **アラートを設定**
   
   **アラート1: 80%到達時**
   ```
   Threshold: 80% of budgeted amount
   Email recipients: drgk2024@kind.ocn.ne.jp
   ```
   
   **アラート2: 100%到達時**
   ```
   Threshold: 100% of budgeted amount
   Email recipients: drgk2024@kind.ocn.ne.jp
   ```
   
   **アラート3: 予測超過時**
   ```
   Threshold: 100% of budgeted amount (Forecasted)
   Email recipients: drgk2024@kind.ocn.ne.jp
   ```

### ステップ2: AWS CLIでの設定（オプション）

```bash
# 予算を作成
aws budgets create-budget \
  --account-id 667481900096 \
  --budget file://budget-config.json \
  --notifications-with-subscribers file://notifications-config.json
```

**budget-config.json:**
```json
{
  "BudgetName": "Monthly-Cost-Limit",
  "BudgetLimit": {
    "Amount": "20",
    "Unit": "USD"
  },
  "TimeUnit": "MONTHLY",
  "BudgetType": "COST"
}
```

**notifications-config.json:**
```json
[
  {
    "Notification": {
      "NotificationType": "ACTUAL",
      "ComparisonOperator": "GREATER_THAN",
      "Threshold": 80,
      "ThresholdType": "PERCENTAGE"
    },
    "Subscribers": [
      {
        "SubscriptionType": "EMAIL",
        "Address": "drgk2024@kind.ocn.ne.jp"
      }
    ]
  },
  {
    "Notification": {
      "NotificationType": "ACTUAL",
      "ComparisonOperator": "GREATER_THAN",
      "Threshold": 100,
      "ThresholdType": "PERCENTAGE"
    },
    "Subscribers": [
      {
        "SubscriptionType": "EMAIL",
        "Address": "drgk2024@kind.ocn.ne.jp"
      }
    ]
  },
  {
    "Notification": {
      "NotificationType": "FORECASTED",
      "ComparisonOperator": "GREATER_THAN",
      "Threshold": 100,
      "ThresholdType": "PERCENTAGE"
    },
    "Subscribers": [
      {
        "SubscriptionType": "EMAIL",
        "Address": "drgk2024@kind.ocn.ne.jp"
      }
    ]
  }
]
```

## 方法2: CloudWatch Billing Alarms

### ステップ1: Billing Metricsを有効化

1. **Billing Consoleにアクセス**
2. 「Billing preferences」を選択
3. 「Receive Billing Alerts」にチェック
4. 保存

### ステップ2: CloudWatch Alarmを作成

```bash
# SNSトピックを作成
aws sns create-topic --name billing-alarm --region us-east-1

# メールアドレスをサブスクライブ
aws sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:667481900096:billing-alarm \
  --protocol email \
  --notification-endpoint drgk2024@kind.ocn.ne.jp \
  --region us-east-1

# アラームを作成（$16で通知）
aws cloudwatch put-metric-alarm \
  --alarm-name billing-alarm-16 \
  --alarm-description "Billing alarm at $16" \
  --metric-name EstimatedCharges \
  --namespace AWS/Billing \
  --statistic Maximum \
  --period 21600 \
  --evaluation-periods 1 \
  --threshold 16 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=Currency,Value=USD \
  --alarm-actions arn:aws:sns:us-east-1:667481900096:billing-alarm \
  --region us-east-1

# アラームを作成（$20で通知）
aws cloudwatch put-metric-alarm \
  --alarm-name billing-alarm-20 \
  --alarm-description "Billing alarm at $20" \
  --metric-name EstimatedCharges \
  --namespace AWS/Billing \
  --statistic Maximum \
  --period 21600 \
  --evaluation-periods 1 \
  --threshold 20 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=Currency,Value=USD \
  --alarm-actions arn:aws:sns:us-east-1:667481900096:billing-alarm \
  --region us-east-1
```

## 方法3: 自動停止Lambda関数（高度）

予算超過時に自動的にリソースを停止するLambda関数を作成

### Lambda関数の作成

```python
import boto3
import os

def lambda_handler(event, context):
    """
    予算超過時にEC2インスタンスとElastic Beanstalk環境を停止
    """
    ec2 = boto3.client('ec2', region_name='ap-northeast-1')
    eb = boto3.client('elasticbeanstalk', region_name='ap-northeast-1')
    
    # Elastic Beanstalk環境を停止
    try:
        eb.update_environment(
            EnvironmentName='health-chat-https-env',
            OptionSettings=[
                {
                    'Namespace': 'aws:autoscaling:asg',
                    'OptionName': 'MinSize',
                    'Value': '0'
                },
                {
                    'Namespace': 'aws:autoscaling:asg',
                    'OptionName': 'MaxSize',
                    'Value': '0'
                }
            ]
        )
        print("Elastic Beanstalk environment scaled down to 0 instances")
    except Exception as e:
        print(f"Error stopping Elastic Beanstalk: {e}")
    
    return {
        'statusCode': 200,
        'body': 'Resources stopped due to budget limit'
    }
```

### EventBridgeルールの作成

```bash
# Lambda関数を作成（上記のコードを使用）
# EventBridgeルールを作成してBudget通知をトリガーに設定
```

## 即座にコストを削減する方法

### 1. 使用していない時間帯にEC2を停止

```bash
# 手動でEC2を停止
aws ec2 stop-instances --instance-ids $(aws ec2 describe-instances \
  --filters "Name=tag:elasticbeanstalk:environment-name,Values=health-chat-https-env" \
  --query 'Reservations[].Instances[].InstanceId' \
  --output text) \
  --region ap-northeast-1
```

### 2. Elastic Beanstalk環境を一時停止

```bash
# 環境を終了（データは保持）
eb terminate health-chat-https-env

# 必要な時に再作成
eb create health-chat-https-env --instance-type t3.small --elb-type application
```

### 3. インスタンスサイズを縮小

```bash
# t3.small → t3.micro に変更（約50%削減）
eb scale 1 --instance-type t3.micro
```

## コスト削減のベストプラクティス

### 現在のコスト内訳（月額）
```
EC2 (t3.small):        $15.00
ALB:                   $18.00
Route 53:              $0.50
S3:                    $1.00
ACM:                   無料
------------------------
合計:                  $34.50/月
```

### 削減策

1. **EC2を停止（夜間・週末）**
   - 12時間/日稼働: $15 → $7.50
   - 節約: $7.50/月

2. **t3.microに変更**
   - $15 → $7.50
   - 節約: $7.50/月

3. **ALBを削除してEC2直接アクセス**
   - $18 → $0
   - 節約: $18/月
   - ⚠️ HTTPSが複雑になる

4. **開発中は環境を削除**
   - 使用時のみ作成
   - 節約: ほぼ全額

### 推奨: 予算内に収める方法

**オプションA: 夜間停止 + t3.micro**
```
EC2 (t3.micro, 12h/日):  $3.75
ALB (削除):              $0
Route 53:                $0.50
S3:                      $1.00
------------------------
合計:                    $5.25/月 ✅
```

**オプションB: 使用時のみ起動**
```
必要な時だけ環境を作成
月10時間使用の場合:      約$2/月 ✅
```

## 今すぐできること

### 1. 予算アラートを設定（5分）

```bash
# AWS Consoleで設定
# https://console.aws.amazon.com/billing/home#/budgets
```

### 2. 今夜からEC2を停止（即座）

```bash
# 毎晩23時に停止、毎朝7時に起動
# EventBridgeで自動化可能
```

### 3. 不要なリソースを削除

```bash
# 使用していないドメイン（lhomrace.com）の自動更新を無効化済み ✅
# 他に不要なリソースがないか確認
aws resourcegroupstaggingapi get-resources --region ap-northeast-1
```

## まとめ

**最も簡単な方法:**
1. AWS Budgetsで$20の予算を設定
2. 80%（$16）と100%（$20）でメール通知
3. 通知を受け取ったら手動でEC2を停止

**自動化する方法:**
1. Lambda関数を作成
2. Budget通知をトリガーに設定
3. 自動的にリソースを停止

**今すぐ実行すべきこと:**
- AWS Budgetsを設定（無料）
- 夜間はEC2を手動で停止
- 週末は環境を削除

これで予算を守りながら、必要な時だけアプリケーションを使用できます！
