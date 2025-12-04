# EC2自動スケジューラー設定完了

**日付**: 2025年12月4日  
**目的**: 使用していない時間帯（18:00-9:00）にEC2インスタンスを自動停止してコスト削減

## 設定内容

### スケジュール
- **停止時刻**: 毎日18:00（JST）
- **起動時刻**: 毎日9:00（JST）
- **停止時間**: 15時間/日
- **稼働時間**: 9時間/日（9:00-18:00）

### コスト削減効果
| 項目 | 24時間稼働 | 9時間稼働 | 削減額 |
|------|-----------|----------|--------|
| EC2 (t3.small) | $14.98/月 | $5.62/月 | **$9.36/月** |
| ALB | $17.50/月 | $17.50/月 | $0 |
| その他 | $1.50/月 | $1.50/月 | $0 |
| **合計** | **$33.98/月** | **$24.62/月** | **$9.36/月 (28%削減)** |

## 作成したAWSリソース

### 1. IAMロール
- **ロール名**: `EC2SchedulerLambdaRole`
- **ARN**: `arn:aws:iam::667481900096:role/EC2SchedulerLambdaRole`
- **権限**:
  - EC2インスタンスの起動・停止
  - Auto Scalingグループの更新
  - CloudWatch Logsへの書き込み

### 2. Lambda関数

#### 停止用Lambda関数
- **関数名**: `StopEC2Instance`
- **ARN**: `arn:aws:lambda:ap-northeast-1:667481900096:function:StopEC2Instance`
- **ランタイム**: Python 3.11
- **動作**: Auto Scalingグループを0インスタンスに設定

#### 起動用Lambda関数
- **関数名**: `StartEC2Instance`
- **ARN**: `arn:aws:lambda:ap-northeast-1:667481900096:function:StartEC2Instance`
- **ランタイム**: Python 3.11
- **動作**: Auto Scalingグループを1インスタンスに設定

### 3. EventBridgeルール

#### 停止ルール
- **ルール名**: `StopEC2At18JST`
- **ARN**: `arn:aws:events:ap-northeast-1:667481900096:rule/StopEC2At18JST`
- **スケジュール**: `cron(0 9 * * ? *)` (UTC 09:00 = JST 18:00)
- **ターゲット**: `StopEC2Instance` Lambda関数

#### 起動ルール
- **ルール名**: `StartEC2At9JST`
- **ARN**: `arn:aws:events:ap-northeast-1:667481900096:rule/StartEC2At9JST`
- **スケジュール**: `cron(0 0 * * ? *)` (UTC 00:00 = JST 09:00)
- **ターゲット**: `StartEC2Instance` Lambda関数

## 動作確認

### テスト結果
✅ **停止テスト**: 成功
- Auto Scalingグループが0インスタンスに設定されました
- 既存のEC2インスタンスが終了しました

✅ **起動テスト**: 成功
- Auto Scalingグループが1インスタンスに設定されました
- 新しいEC2インスタンスが起動しました（約1-2分）

### 確認コマンド

#### 現在のAuto Scalingグループの状態を確認
```bash
aws autoscaling describe-auto-scaling-groups \
  --auto-scaling-group-names awseb-e-qxcmrgmqev-stack-AWSEBAutoScalingGroup-5NdTtAsOQSs7 \
  --region ap-northeast-1 \
  --query 'AutoScalingGroups[0].[MinSize,MaxSize,DesiredCapacity]' \
  --output text
```

#### 現在のEC2インスタンスの状態を確認
```bash
aws ec2 describe-instances \
  --filters "Name=tag:elasticbeanstalk:environment-name,Values=health-chat-https-env" \
  --region ap-northeast-1 \
  --query 'Reservations[].Instances[].[InstanceId,State.Name,LaunchTime]' \
  --output table
```

#### EventBridgeルールの状態を確認
```bash
aws events list-rules \
  --region ap-northeast-1 \
  --query 'Rules[?contains(Name, `EC2`)].[Name,State,ScheduleExpression]' \
  --output table
```

## 手動操作

### 手動で停止する場合
```bash
aws lambda invoke \
  --function-name StopEC2Instance \
  --region ap-northeast-1 \
  response.json && cat response.json
```

### 手動で起動する場合
```bash
aws lambda invoke \
  --function-name StartEC2Instance \
  --region ap-northeast-1 \
  response.json && cat response.json
```

### スケジュールを一時的に無効化する場合
```bash
# 停止ルールを無効化
aws events disable-rule --name StopEC2At18JST --region ap-northeast-1

# 起動ルールを無効化
aws events disable-rule --name StartEC2At9JST --region ap-northeast-1
```

### スケジュールを再度有効化する場合
```bash
# 停止ルールを有効化
aws events enable-rule --name StopEC2At18JST --region ap-northeast-1

# 起動ルールを有効化
aws events enable-rule --name StartEC2At9JST --region ap-northeast-1
```

## スケジュールの変更方法

### 停止時刻を変更する場合（例: 20:00に変更）
```bash
# UTC 11:00 = JST 20:00
aws events put-rule \
  --name StopEC2At18JST \
  --schedule-expression "cron(0 11 * * ? *)" \
  --region ap-northeast-1
```

### 起動時刻を変更する場合（例: 7:00に変更）
```bash
# UTC 22:00 (前日) = JST 07:00
aws events put-rule \
  --name StartEC2At9JST \
  --schedule-expression "cron(0 22 * * ? *)" \
  --region ap-northeast-1
```

### 週末も停止する場合
現在の設定では、土日も含めて毎日18:00に停止、9:00に起動します。
平日のみ稼働させたい場合は、cronの曜日指定を変更します。

```bash
# 平日のみ起動（月-金の9:00）
aws events put-rule \
  --name StartEC2At9JST \
  --schedule-expression "cron(0 0 ? * MON-FRI *)" \
  --region ap-northeast-1

# 平日のみ停止（月-金の18:00）
aws events put-rule \
  --name StopEC2At18JST \
  --schedule-expression "cron(0 9 ? * MON-FRI *)" \
  --region ap-northeast-1
```

## 注意事項

### 1. タイムゾーン
- EventBridgeのcron式はUTC時刻で指定します
- 日本時間（JST）はUTC+9時間です
- JST 18:00 = UTC 09:00
- JST 09:00 = UTC 00:00

### 2. 起動時間
- EC2インスタンスの起動には約1-2分かかります
- 起動後、アプリケーションが完全に利用可能になるまでさらに1-2分かかる場合があります

### 3. データの永続性
- S3に保存されたデータは、EC2停止中も保持されます
- セッション情報は失われるため、停止中にアクセスしたユーザーは再ログインが必要です

### 4. ヘルスチェック
- EC2停止中、ALBのヘルスチェックは失敗します
- これは正常な動作です
- 起動後、自動的にヘルスチェックが成功します

### 5. コスト
- EC2停止中は、EC2の料金は発生しません
- ALBは常時稼働のため、料金が発生し続けます
- S3、Route 53の料金も継続します

## トラブルシューティング

### 問題1: スケジュール通りに停止・起動しない

**確認事項:**
1. EventBridgeルールが有効になっているか確認
   ```bash
   aws events describe-rule --name StopEC2At18JST --region ap-northeast-1
   ```

2. Lambda関数のログを確認
   ```bash
   aws logs tail /aws/lambda/StopEC2Instance --region ap-northeast-1 --follow
   ```

### 問題2: 起動後にアプリケーションにアクセスできない

**確認事項:**
1. EC2インスタンスが起動しているか確認
2. ヘルスチェックが成功しているか確認（2-3分待つ）
3. セキュリティグループの設定を確認

### 問題3: 予期しないインスタンスが起動する

**原因**: Elastic Beanstalkの自動スケーリングが有効
**解決**: Lambda関数がAuto Scalingグループを制御するため、この問題は発生しません

## Lambda関数のコード

### ec2_scheduler_v2.py
```python
import boto3
import os

ec2 = boto3.client('ec2', region_name='ap-northeast-1')
autoscaling = boto3.client('autoscaling', region_name='ap-northeast-1')

def lambda_handler(event, context):
    """
    EC2インスタンスを停止または起動する（Auto Scaling対応）
    環境変数:
    - ACTION: 'stop' または 'start'
    - ENVIRONMENT_NAME: Elastic Beanstalk環境名
    """
    action = os.environ.get('ACTION', 'stop')
    environment_name = os.environ.get('ENVIRONMENT_NAME', 'health-chat-https-env')
    
    # Auto Scaling Groupを取得
    asg_response = autoscaling.describe_auto_scaling_groups()
    asg_name = None
    
    for asg in asg_response['AutoScalingGroups']:
        for tag in asg['Tags']:
            if tag['Key'] == 'elasticbeanstalk:environment-name' and tag['Value'] == environment_name:
                asg_name = asg['AutoScalingGroupName']
                break
        if asg_name:
            break
    
    if not asg_name:
        print(f"No Auto Scaling Group found for environment: {environment_name}")
        return {
            'statusCode': 404,
            'body': f'No Auto Scaling Group found for environment: {environment_name}'
        }
    
    print(f"Found Auto Scaling Group: {asg_name}")
    
    # インスタンスを停止または起動
    if action == 'stop':
        # Auto Scalingを0に設定
        autoscaling.update_auto_scaling_group(
            AutoScalingGroupName=asg_name,
            MinSize=0,
            MaxSize=0,
            DesiredCapacity=0
        )
        print(f"Set Auto Scaling Group {asg_name} to 0 instances")
        return {
            'statusCode': 200,
            'body': f'Set Auto Scaling Group {asg_name} to 0 instances'
        }
    elif action == 'start':
        # Auto Scalingを1に設定
        autoscaling.update_auto_scaling_group(
            AutoScalingGroupName=asg_name,
            MinSize=1,
            MaxSize=1,
            DesiredCapacity=1
        )
        print(f"Set Auto Scaling Group {asg_name} to 1 instance")
        return {
            'statusCode': 200,
            'body': f'Set Auto Scaling Group {asg_name} to 1 instance'
        }
    else:
        print(f"Invalid action: {action}")
        return {
            'statusCode': 400,
            'body': f'Invalid action: {action}'
        }
```

## まとめ

✅ **設定完了**
- 毎日18:00にEC2インスタンスが自動停止
- 毎日9:00にEC2インスタンスが自動起動
- 月額約$9.36のコスト削減（28%削減）
- 新しい月額コスト: 約$24.62

✅ **動作確認済み**
- 停止・起動のテストが成功
- Auto Scalingグループの制御が正常に動作

✅ **運用開始**
- 本日（2025年12月4日）18:00から自動停止が開始されます
- 明日（2025年12月5日）9:00から自動起動が開始されます

---

**次のステップ（オプション）:**
- 週末も停止する設定に変更（さらに$1-2削減）
- t3.microに変更（さらに$3-4削減）
- モニタリングアラートの設定
