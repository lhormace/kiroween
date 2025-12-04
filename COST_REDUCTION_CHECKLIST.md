# コスト削減チェックリスト

## 🎯 目的
開発作業終了時にAWSリソースを適切に停止・削除してコストを削減する

## 📋 実施項目

### 1. Elastic Beanstalk 環境の停止
**現在の状態**: ✅ 実行中
- 環境名: health-chat-env
- インスタンスタイプ: t3.small
- 推定コスト: 約 $0.0208/時間 = 約 $15/月

**オプション**:

#### オプションA: 環境の終了（推奨 - 完全停止）
```bash
eb terminate health-chat-env --force
```
**メリット**:
- コストが完全にゼロになる
- S3バケットのデータは保持される

**デメリット**:
- 再開時に環境を再作成する必要がある（約5分）
- IPアドレスが変わる

#### オプションB: 環境の保持（開発継続時）
環境を保持したまま、定期的に停止・起動するスケジュールを設定
- 現在の環境を維持
- 必要な時だけ起動

### 2. S3 バケットの確認
**バケット名**: health-chat-data
**現在の使用量**: ほぼ空（テストデータのみ）
**推定コスト**: $0.023/GB/月（ほぼ無料）

**推奨**: 保持（データは小さく、コストも低い）

### 3. CloudWatch Logs の確認
**ログの保持期間**: デフォルト（無期限）
**推奨**: 
```bash
# ログの保持期間を30日に設定（コスト削減）
aws logs put-retention-policy \
  --log-group-name /aws/elasticbeanstalk/health-chat-env/var/log/web.stdout.log \
  --retention-in-days 30
```

### 4. 未使用のElastic IPの確認
```bash
aws ec2 describe-addresses --region ap-northeast-1 --query 'Addresses[?AssociationId==null]'
```

### 5. 未使用のEBSボリュームの確認
```bash
aws ec2 describe-volumes --region ap-northeast-1 --filters Name=status,Values=available
```

## 🔧 今回の実施内容

### 実施日時: 2025-12-04 01:15 JST

#### ステップ1: 環境の終了
```bash
eb terminate health-chat-env --force
```

**実行結果**:
```
2025-12-03 16:15:19    INFO    terminateEnvironment is starting.
2025-12-03 16:15:19    INFO    Validating environment's EC2 instances have termination protection disabled
2025-12-03 16:15:20    INFO    Finished validating environment's EC2 instances for termination protection.
2025-12-03 16:15:37    INFO    Waiting for EC2 instances to terminate. This may take a few minutes.
2025-12-03 16:18:40    INFO    Deleted EIP: 3.113.95.51
2025-12-03 16:18:40    INFO    Deleted security group named: awseb-e-rpjhhdrdpm-stack-AWSEBSecurityGroup-*
2025-12-03 16:18:43    INFO    Deleting SNS topic for environment health-chat-env.
2025-12-03 16:18:44    INFO    terminateEnvironment completed successfully.
```

✅ **完了**: Elastic Beanstalk環境が正常に終了されました

#### ステップ2: 未使用リソースの確認

**Elastic IP**: ✅ なし（すべて削除済み）
**未使用EBSボリューム**: ✅ なし（すべて削除済み）

#### ステップ3: S3バケットの確認
```bash
aws s3 ls s3://health-chat-data/
```
**状態**: 保持（データは小さく、将来の開発で使用）

## 📊 コスト削減効果

### 削減前（実行中）
- EC2 t3.small: $0.0208/時間 × 24時間 × 30日 = **約 $15/月**
- EBS 8GB: $0.10/GB/月 × 8GB = **約 $0.80/月**
- Elastic IP（使用中）: $0/月
- **合計: 約 $15.80/月**

### 削減後（終了後）
- EC2: **$0/月**
- EBS: **$0/月**
- S3（ほぼ空）: **約 $0.01/月**
- **合計: 約 $0.01/月**

### 削減額
**約 $15.79/月（約 99.9% 削減）**

## 🔄 次回の起動方法

### 環境の再作成
```bash
# 1. 最新のコードを取得
git pull origin main

# 2. アプリケーションをビルド
mvn clean package -DskipTests

# 3. JARファイルをコピー
cp target/application.jar .

# 4. Elastic Beanstalk環境を作成
eb create health-chat-env \
  --single \
  --instance-type t3.small \
  --platform "64bit Amazon Linux 2 v3.10.0 running Corretto 17" \
  --timeout 20

# 5. 環境変数を設定
eb setenv \
  S3_BUCKET_NAME=health-chat-data \
  AWS_REGION=ap-northeast-1 \
  LOGGING_LEVEL_COM_HEALTH_CHAT=DEBUG \
  LOGGING_LEVEL_ROOT=INFO

# 6. 動作確認
eb open
```

**所要時間**: 約5-7分

### 注意事項
- 新しいIPアドレスが割り当てられます
- URLは同じ（health-chat-env.eba-xaqnxjtp.ap-northeast-1.elasticbeanstalk.com）
- S3のデータは保持されています
- IAMロールの設定は保持されています

## 📝 保持されているリソース

### AWS リソース
1. **S3 バケット**: health-chat-data
   - ユーザーデータ（将来使用）
   - コスト: ほぼ無料

2. **IAM ロール**: aws-elasticbeanstalk-ec2-role
   - 設定済みポリシー:
     - AWSElasticBeanstalkWebTier
     - AWSElasticBeanstalkWorkerTier
     - AWSElasticBeanstalkMulticontainerDocker
     - AmazonS3FullAccess

3. **Elastic Beanstalk アプリケーション**: health-chat-advisor
   - 環境は削除されたが、アプリケーション定義は保持

### GitHub リポジトリ
- **リポジトリ**: https://github.com/lhormace/kiroween
- **最新コミット**: 8cdf523 "Add debug logging for user registration troubleshooting"
- **ブランチ**: main

## ✅ 完了チェックリスト

- [x] Elastic Beanstalk環境の終了
- [x] 未使用Elastic IPの確認（なし）
- [x] 未使用EBSボリュームの確認（なし）
- [x] S3バケットの確認（保持）
- [x] コスト削減効果の計算
- [x] 次回起動手順の文書化
- [x] GitHubへのコミット
- [x] 引き継ぎメモの作成

## 🎉 完了

すべてのコスト削減対策が完了しました。
次回の開発セッション開始時は、上記の「次回の起動方法」に従って環境を再作成してください。
