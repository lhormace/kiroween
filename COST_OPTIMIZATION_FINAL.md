# コスト最適化 - 最終設定

**日付**: 2025年12月4日  
**目的**: 月額コストを予算$20に近づける

## 実施した設定

### ✅ 週末停止スケジュール

**稼働時間:**
- 平日（月-金）: 9:00-18:00（9時間/日）
- 週末（土-日）: 終日停止

**スケジュール:**
- 起動: 平日9:00（JST） = `cron(0 0 ? * MON-FRI *)`
- 停止: 平日18:00（JST） = `cron(0 9 ? * MON-FRI *)`

**月間稼働時間:**
- 9時間/日 × 22日（平日） = 198時間/月

### ✅ インスタンスタイプ

**決定: t3.small**

**理由:**
- t3.micro（1GB RAM）ではSpring Bootアプリケーションが起動しない
- t3.small（2GB RAM）で安定動作
- メモリ不足によるクラッシュを回避

## コスト内訳

### 月額コスト（最終）

| 項目 | 単価 | 使用量 | 月額 |
|------|------|--------|------|
| EC2 (t3.small) | $0.0208/時間 | 198時間 | **$4.12** |
| ALB | $0.0243/時間 | 720時間 | **$17.50** |
| Route 53 | - | - | **$0.50** |
| S3 | - | - | **$1.00** |
| **合計** | - | - | **$23.12/月** |

### コスト削減効果

| 項目 | 24時間稼働 | 週末停止 | 削減額 |
|------|-----------|----------|--------|
| EC2 | $14.98 | $4.12 | **$10.86** |
| その他 | $19.00 | $19.00 | $0 |
| **合計** | **$33.98** | **$23.12** | **$10.86 (32%削減)** |

### 予算との比較

- **予算**: $20.00/月
- **実際**: $23.12/月
- **差額**: +$3.12/月（予算の15.6%超過）

## 作成したAWSリソース

### Lambda関数
1. **StopEC2Instance**
   - ARN: `arn:aws:lambda:ap-northeast-1:667481900096:function:StopEC2Instance`
   - 動作: Auto Scalingグループを0インスタンスに設定

2. **StartEC2Instance**
   - ARN: `arn:aws:lambda:ap-northeast-1:667481900096:function:StartEC2Instance`
   - 動作: Auto Scalingグループを1インスタンスに設定

### EventBridgeルール
1. **StopEC2At18JST**
   - スケジュール: 平日18:00（JST）
   - ターゲット: StopEC2Instance Lambda関数

2. **StartEC2At9JST**
   - スケジュール: 平日9:00（JST）
   - ターゲット: StartEC2Instance Lambda関数

### IAMロール
- **EC2SchedulerLambdaRole**
  - ARN: `arn:aws:iam::667481900096:role/EC2SchedulerLambdaRole`
  - 権限: EC2、Auto Scaling、CloudWatch Logs

## 運用開始

### 初回スケジュール
- **今日（金曜日）18:00**: 初回の自動停止
- **月曜日9:00**: 週末明けの自動起動

### 動作確認
✅ 環境状態: Ready / Green  
✅ インスタンスタイプ: t3.small  
✅ スケジュール: 有効  
✅ アプリケーション: 正常動作  

## 手動操作

### 緊急時の手動起動
```bash
aws lambda invoke \
  --function-name StartEC2Instance \
  --region ap-northeast-1 \
  response.json
```

### 緊急時の手動停止
```bash
aws lambda invoke \
  --function-name StopEC2Instance \
  --region ap-northeast-1 \
  response.json
```

### スケジュールの一時無効化
```bash
# 停止スケジュールを無効化（週末も稼働させたい場合）
aws events disable-rule --name StopEC2At18JST --region ap-northeast-1

# 起動スケジュールを無効化
aws events disable-rule --name StartEC2At9JST --region ap-northeast-1
```

### スケジュールの再有効化
```bash
aws events enable-rule --name StopEC2At18JST --region ap-northeast-1
aws events enable-rule --name StartEC2At9JST --region ap-northeast-1
```

## さらにコストを削減する方法

予算$20に収めるには、さらに約$3の削減が必要です。

### オプション1: 稼働時間を短縮
- 現在: 9:00-18:00（9時間）
- 変更案: 10:00-17:00（7時間）
- 削減額: 約$0.92/月

### オプション2: ALBを削除してEC2直接アクセス
- 削減額: $17.50/月
- デメリット: HTTPSの設定が複雑、スケーラビリティなし

### オプション3: 使用時のみ環境を起動（完全手動）
- 削減額: 最大$30/月
- デメリット: 毎回10-15分の起動時間、完全手動

## 注意事項

### 1. 停止中のアクセス
- EC2停止中（平日18:00-翌9:00、週末終日）はアプリケーションにアクセスできません
- ALBは稼働していますが、バックエンドがないため503エラーが返されます

### 2. データの永続性
- S3に保存されたデータは、EC2停止中も保持されます
- セッション情報は失われるため、停止中にアクセスしたユーザーは再ログインが必要です

### 3. 起動時間
- EC2インスタンスの起動には約1-2分かかります
- アプリケーションが完全に利用可能になるまでさらに1-2分かかります

### 4. ヘルスチェック
- EC2停止中、ALBのヘルスチェックは失敗します（正常な動作）
- 起動後、自動的にヘルスチェックが成功します

## トラブルシューティング

### 問題1: スケジュール通りに起動・停止しない

**確認コマンド:**
```bash
# EventBridgeルールの状態を確認
aws events describe-rule --name StopEC2At18JST --region ap-northeast-1
aws events describe-rule --name StartEC2At9JST --region ap-northeast-1

# Lambda関数のログを確認
aws logs tail /aws/lambda/StopEC2Instance --region ap-northeast-1 --follow
```

### 問題2: 起動後にアプリケーションにアクセスできない

**確認手順:**
1. EC2インスタンスが起動しているか確認
2. 2-3分待ってヘルスチェックが成功するか確認
3. `https://lhormace.com`でアクセス

### 問題3: 環境がRed状態になる

**対処法:**
```bash
# アプリケーションを再デプロイ
mvn clean package -DskipTests
eb deploy health-chat-https-env
```

## まとめ

✅ **設定完了**
- 週末停止スケジュール: 有効
- インスタンスタイプ: t3.small
- 月額コスト: $23.12（予算$20から+$3.12）
- コスト削減: $10.86/月（32%削減）

✅ **運用開始**
- 本日18:00から自動停止開始
- 平日9:00-18:00のみ稼働

✅ **次のステップ（オプション）**
- 稼働時間をさらに短縮（予算$20に近づける）
- モニタリングアラートの設定
- バックアップ戦略の構築

---

**最終更新**: 2025年12月4日 23:20 JST
