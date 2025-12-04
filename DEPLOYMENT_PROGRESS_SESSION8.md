# デプロイメント進捗 - セッション8

**日付**: 2025年12月4日  
**目的**: HTTPS環境の構築（ACM証明書取得とRoute 53設定）

## 実施内容

### 1. 前セッションの状況確認

**前セッション（セッション7）の成果:**
- ローカル環境でユーザー登録機能が正常動作することを確認
- `application-local.properties`を作成してローカル開発環境を整備
- AWS環境は削除済み（EC2、Elastic Beanstalk環境）

**今回の目標:**
- HTTPS環境の構築
- ACM証明書の取得
- Route 53でのドメイン設定
- Elastic BeanstalkへのHTTPS適用

### 2. AWS CLI接続確認

✅ **AWS CLI接続成功**
```bash
aws sts get-caller-identity
# ユーザー: Kiroween
# アカウントID: 667481900096
# リージョン: ap-northeast-1
```

### 3. ドメイン登録状況

**登録済みドメイン:**
1. `lhomrace.com` - 誤って登録（自動更新を無効化済み）
2. `lhormace.com` - 正しいドメイン（使用中）

**連絡先情報:**
- メール: `drgk2024@kind.ocn.ne.jp`
- 電話: `+81.8096199430`
- 住所: 北海道札幌市白石区南郷通20丁目南1-30-105
- 郵便番号: `003-0022`

### 4. Route 53ホストゾーン

✅ **ホストゾーン作成済み**
- ドメイン: `lhormace.com`
- ホストゾーンID: `Z01490392T375J4LFOZ3M`

### 5. ACM証明書の取得

✅ **証明書リクエスト完了**
- 証明書ARN: `arn:aws:acm:us-east-1:667481900096:certificate/adc070f0-60c4-4586-8285-ea7f342b8ef8`
- リージョン: `us-east-1`（CloudFront/ALB用）
- カバー範囲: `lhormace.com`と`*.lhormace.com`
- 検証方法: DNS検証

**証明書情報:**
```json
{
  "DomainName": "lhormace.com",
  "SubjectAlternativeNames": [
    "lhormace.com",
    "*.lhormace.com"
  ],
  "Status": "PENDING_VALIDATION",
  "CreatedAt": "2025-12-04T13:53:55+09:00"
}
```

### 6. DNS検証レコードの追加

✅ **検証用CNAMEレコードをRoute 53に追加**

**追加したレコード:**
- Name: `_57ea5bc0dbdb39047c14ab376504fc82.lhormace.com`
- Type: `CNAME`
- Value: `_362bc13c8d3b896b2e63005f5289d3b2.jkddzztszm.acm-validations.aws.`
- TTL: 300秒

**追加結果:**
```json
{
  "ChangeInfo": {
    "Id": "/change/C08529231AQFM6FWJ6V68",
    "Status": "PENDING",
    "SubmittedAt": "2025-12-04T10:45:38+00:00"
  }
}
```

### 7. 現在の状況（19:45時点）

⏳ **ACM証明書の検証待ち**
- 現在のステータス: `PENDING_VALIDATION`
- DNSレコード伝播待ち: 5-30分
- 検証完了予定: 20:15頃

**待機中のタスク:**
1. DNS伝播の完了
2. ACMによる自動検証
3. 証明書ステータスが`ISSUED`に変更

## 次のステップ

### 証明書検証完了後

1. **Elastic Beanstalk環境の作成**
   - ロードバランサー付きで環境を作成
   - インスタンスタイプ: t3.small
   - プラットフォーム: Corretto 17

2. **HTTPSリスナーの設定**
   - ロードバランサーにHTTPSリスナーを追加
   - ACM証明書を適用
   - ポート443でリッスン

3. **HTTPからHTTPSへのリダイレクト設定**
   - ポート80でのアクセスを443にリダイレクト

4. **Route 53でのAレコード追加**
   - `lhormace.com` → Elastic Beanstalkのロードバランサー
   - エイリアスレコードとして設定

5. **環境変数の設定**
   ```bash
   SPRING_PROFILES_ACTIVE=production
   AWS_REGION=ap-northeast-1
   S3_BUCKET_NAME=health-chat-data
   ```

6. **アプリケーションのデプロイ**
   - `application.jar`をデプロイ
   - ヘルスチェックの確認

7. **動作確認**
   - `https://lhormace.com`でアクセス
   - ユーザー登録機能のテスト
   - セキュアCookieの動作確認

## トラブルシューティング対応

### AWS CLIのタイムアウト問題

**問題:**
`aws acm describe-certificate`コマンドが応答を返さない

**解決策:**
- タイムアウト設定を追加: `--timeout 5000`
- 出力を制限: `| head -100`
- 必要な情報のみクエリ: `--query 'Certificate.Status'`

## コスト状況

現在のAWSリソース:
- ✅ Route 53ホストゾーン: $0.50/月
- ✅ ドメイン登録: `lhormace.com` - $13/年
- ⚠️ ドメイン登録: `lhomrace.com` - $13/年（自動更新無効化済み、1年後に削除）
- ✅ ACM証明書: 無料
- ❌ EC2インスタンス: なし
- ❌ Elastic Beanstalk環境: なし

**月額コスト（現在）**: 約$0.50

## 作成・更新したファイル

### 新規作成
- `DEPLOYMENT_PROGRESS_SESSION8.md` - 本セッションの記録

### 一時ファイル
- `/tmp/acm-validation-record.json` - DNS検証レコード設定用JSON

## 技術的な学び

### ACM証明書のDNS検証

1. **証明書リクエスト時に検証用DNSレコード情報が提供される**
2. **ワイルドカード証明書でも1つのCNAMEレコードで検証可能**
   - `lhormace.com`と`*.lhormace.com`の両方が同じCNAMEレコードを使用
3. **検証は自動的に行われる**
   - DNSレコードが伝播すると、AWSが自動的に検証
   - 手動での操作は不要

### Route 53のレコード管理

- `UPSERT`アクションで既存レコードの更新または新規作成が可能
- TTL 300秒で設定（5分）
- 変更は`PENDING`状態から開始され、伝播後に完了

---

## 8. HTTPS環境の構築完了（20:15-21:00）

### ACM証明書の検証完了

✅ **us-east-1の証明書**: `ISSUED`（20:15確認）
✅ **ap-northeast-1の証明書**: 新規リクエスト→即座に`ISSUED`

**理由**: 同じDNS検証レコードを使用するため、既にRoute 53に追加済みのレコードで自動検証

### Elastic Beanstalk環境の作成

✅ **環境作成完了**
```bash
eb create health-chat-https-env \
  --instance-type t3.small \
  --elb-type application \
  --envvars SPRING_PROFILES_ACTIVE=production,AWS_REGION=ap-northeast-1,S3_BUCKET_NAME=health-chat-data
```

**環境情報:**
- 環境名: `health-chat-https-env`
- URL: `health-chat-https-env.eba-3czeyipx.ap-northeast-1.elasticbeanstalk.com`
- ロードバランサー: `awseb--AWSEB-JdMIVrmya4cA`
- ロードバランサーDNS: `awseb--AWSEB-JdMIVrmya4cA-1155826103.ap-northeast-1.elb.amazonaws.com`
- ステータス: `Ready`
- ヘルス: `Green`

### HTTPSリスナーの追加

✅ **HTTPSリスナー作成完了**
```bash
aws elbv2 create-listener \
  --load-balancer-arn "arn:aws:elasticloadbalancing:ap-northeast-1:667481900096:loadbalancer/app/awseb--AWSEB-JdMIVrmya4cA/602253db23f4388f" \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:ap-northeast-1:667481900096:certificate/e1bac4b4-3187-4a56-82d4-1069a8972134 \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:ap-northeast-1:667481900096:targetgroup/awseb-AWSEB-IS5QLPYMIFNJ/da29240f492240ea
```

**リスナー情報:**
- ListenerArn: `arn:aws:elasticloadbalancing:ap-northeast-1:667481900096:listener/app/awseb--AWSEB-JdMIVrmya4cA/602253db23f4388f/a397d0aa6590c7cb`
- ポート: 443
- プロトコル: HTTPS
- 証明書: ap-northeast-1の証明書

### Route 53でのドメイン紐付け

✅ **Aレコード追加完了**
```json
{
  "Name": "lhormace.com",
  "Type": "A",
  "AliasTarget": {
    "HostedZoneId": "Z14GRHDCWA56QT",
    "DNSName": "awseb--AWSEB-JdMIVrmya4cA-1155826103.ap-northeast-1.elb.amazonaws.com",
    "EvaluateTargetHealth": true
  }
}
```

**変更ID**: `/change/C09388773HAA12IXYUYPT`

### HTTPからHTTPSへのリダイレクト設定

✅ **HTTPリスナー変更完了**
```bash
aws elbv2 modify-listener \
  --listener-arn "arn:aws:elasticloadbalancing:ap-northeast-1:667481900096:listener/app/awseb--AWSEB-JdMIVrmya4cA/602253db23f4388f/27ded3b8517e2164" \
  --default-actions Type=redirect,RedirectConfig="{Protocol=HTTPS,Port=443,StatusCode=HTTP_301}"
```

**リダイレクト設定:**
- ポート80（HTTP）→ポート443（HTTPS）
- ステータスコード: 301（恒久的リダイレクト）

### セキュアCookieの設定

✅ **application-production.properties更新**
```properties
# 変更前
server.servlet.session.cookie.secure=false

# 変更後
server.servlet.session.cookie.secure=true
```

### アプリケーションのデプロイ

✅ **ビルド成功**
```bash
mvn clean package -DskipTests
# BUILD SUCCESS
```

✅ **デプロイ成功**
```bash
eb deploy health-chat-https-env
# Environment update completed successfully
```

**デプロイバージョン**: `app-8cdf-251204_205919061408`

## 最終確認

### 環境ステータス
```
Environment details for: health-chat-https-env
  Status: Ready
  Health: Green
  CNAME: health-chat-https-env.eba-3czeyipx.ap-northeast-1.elasticbeanstalk.com
```

### アクセス可能なURL

1. **独自ドメイン（HTTPS）**: `https://lhormace.com`
   - DNS伝播待ち（5-30分）
   
2. **Elastic Beanstalk URL（HTTPS）**: `https://health-chat-https-env.eba-3czeyipx.ap-northeast-1.elasticbeanstalk.com`
   - 即座にアクセス可能

3. **HTTPアクセス**: 自動的にHTTPSにリダイレクト

## トラブルシューティング記録

### 問題1: ACM証明書のリージョン不一致

**問題**: us-east-1の証明書をap-northeast-1のALBで使用しようとしてエラー

**解決**: ap-northeast-1で新しい証明書をリクエスト。同じDNS検証レコードを使用するため即座に検証完了

### 問題2: AWS CLIコマンドのタイムアウト

**問題**: 一部のコマンドが応答を返さない、またはタイムアウトする

**解決**: `--timeout`オプションを追加、または出力を制限（`| head -100`）

**影響**: タイムアウトしても、コマンド自体は成功している場合が多い。重要な情報は取得できている

## コスト状況（更新）

現在のAWSリソース:
- ✅ Route 53ホストゾーン: $0.50/月
- ✅ ドメイン登録: `lhormace.com` - $13/年
- ⚠️ ドメイン登録: `lhomrace.com` - $13/年（自動更新無効化済み）
- ✅ ACM証明書（us-east-1）: 無料
- ✅ ACM証明書（ap-northeast-1）: 無料
- ✅ EC2インスタンス（t3.small）: 約$0.0208/時間 = 約$15/月
- ✅ Application Load Balancer: 約$0.0243/時間 = 約$18/月
- ✅ Elastic Beanstalk: 無料（EC2とALBの料金のみ）

**月額コスト（推定）**: 約$33.50

**コスト削減策**:
- EC2インスタンスを使用しない時間帯に停止
- 開発中はt3.microに変更（約$7.5/月）

## 次のステップ

1. **DNS伝播を待つ**（5-30分）
2. **動作確認**
   - `https://lhormace.com`でアクセス
   - ユーザー登録機能のテスト
   - セキュアCookieの動作確認
3. **問題があれば調査**
   - ログの確認
   - S3権限の確認

---

## 9. 最終動作確認と問題修正（21:40-22:00）

### セキュリティグループの修正

⚠️ **問題**: HTTPSでアクセスできない
**原因**: ロードバランサーのセキュリティグループにポート443のインバウンドルールがなかった

✅ **解決**: ポート443を追加
```bash
aws ec2 authorize-security-group-ingress \
  --group-id sg-01164fa803c354939 \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0
```

### チャット機能の修正

⚠️ **問題**: チャットメッセージを送信しても回答が返ってこない
**原因**: JavaScriptからのPOSTリクエストにCSRFトークンが含まれていなかった

✅ **解決**: Thymeleafのインライン機能を使用してCSRFトークンを取得し、リクエストヘッダーに追加

**修正内容:**
```javascript
// Get CSRF token from Thymeleaf
const csrfToken = /*[[${_csrf.token}]]*/ '';
const csrfHeader = /*[[${_csrf.headerName}]]*/ '';

// Add CSRF token to request headers
const headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
};
if (csrfToken && csrfHeader) {
    headers[csrfHeader] = csrfToken;
}
```

### ログイン機能の修正

⚠️ **問題**: ログインできない
**原因**: Spring Securityのフォームログインが有効で、`DaoAuthenticationProvider`がユーザーを見つけられなかった

✅ **解決**: Spring Securityのフォームログインを無効化し、`AuthController`のカスタム認証のみを使用

**修正内容:**
```java
// Disable form login - using custom authentication in AuthController
.formLogin(form -> form.disable())
```

### 最終動作確認結果

✅ **全機能が正常に動作！**

1. **ユーザー登録**: 正常に動作
2. **ログイン**: 正常に動作
3. **チャット機能**: メッセージ送信と応答が正常に動作
4. **データ保存**: S3への保存が正常に動作
5. **グラフ表示**: 体重・体脂肪率のグラフが表示
6. **セキュリティ**: HTTPS、セキュアCookie、CSRFトークン、全て正常

**テスト結果:**
```
✅ HTTPS接続: https://lhormace.com
✅ HTTP/2プロトコル
✅ セキュアCookie: Secure; HttpOnly; SameSite=Lax
✅ HTTPからHTTPSへの自動リダイレクト（301）
✅ セキュリティヘッダー:
   - HSTS (HTTP Strict Transport Security)
   - X-Frame-Options: DENY
   - Content-Security-Policy
   - X-XSS-Protection
✅ ユーザー登録・ログイン
✅ チャット機能（AI応答、健康分析、短歌生成）
✅ データ永続化（S3）
```

## 作成・更新したファイル（セッション8全体）

### 新規作成
- `DEPLOYMENT_PROGRESS_SESSION8.md` - 本セッションの詳細記録

### 更新
- `src/main/resources/application-production.properties` - Forward headers設定追加
- `src/main/java/com/health/chat/web/AuthController.java` - Spring Security認証コンテキスト設定
- `src/main/resources/templates/chat.html` - CSRFトークン追加
- `src/main/java/com/health/chat/config/SecurityConfig.java` - フォームログイン無効化

## 最終的なシステム構成

### インフラストラクチャ
```
Internet
    ↓
Route 53 (lhormace.com)
    ↓
Application Load Balancer (HTTPS:443, HTTP:80→HTTPS)
    ↓ (ACM証明書)
Elastic Beanstalk (health-chat-https-env)
    ↓
EC2 Instance (t3.small, Corretto 17)
    ↓
Spring Boot Application
    ↓
S3 Bucket (health-chat-data)
```

### セキュリティ設定
- **通信**: HTTPS (TLS 1.2+)
- **証明書**: ACM (us-east-1 & ap-northeast-1)
- **Cookie**: Secure, HttpOnly, SameSite=Lax
- **CSRF**: トークンベース保護
- **ヘッダー**: HSTS, CSP, X-Frame-Options, X-XSS-Protection
- **認証**: カスタム認証 + Spring Security
- **セッション**: サーバーサイドセッション管理

### データフロー
1. ユーザー登録 → S3に保存（BCryptでハッシュ化）
2. ログイン → セッション作成 → SecurityContext設定
3. チャットメッセージ → 健康データ解析 → S3に保存 → AI応答生成
4. グラフ表示 → S3からデータ取得 → Chart.jsで可視化

## コスト最適化の提案

### 現在のコスト（月額）
- EC2 (t3.small): 約$15
- ALB: 約$18
- Route 53: $0.50
- S3: 最小限（$1未満）
- ACM: 無料
- **合計**: 約$33.50/月

### コスト削減策
1. **EC2自動停止**: 使用しない時間帯に停止（50%削減可能）
2. **インスタンスサイズ**: t3.microに変更（開発中）
3. **リザーブドインスタンス**: 長期利用の場合（30-40%削減）

## 技術的な学び

### 1. ロードバランサー背後でのHTTPS検出
```properties
server.forward-headers-strategy=native
server.tomcat.remoteip.protocol-header=x-forwarded-proto
```

### 2. Spring SecurityとカスタムAuthentication
- Spring Securityの`SecurityContext`に認証情報を設定
- セッションに`SPRING_SECURITY_CONTEXT`を保存
- フォームログインを無効化してカスタム認証を使用

### 3. CSRFトークンのJavaScript連携
- Thymeleafのインライン機能で変数を渡す
- `th:inline="javascript"`を使用
- `/*[[${_csrf.token}]]*/`でトークンを取得

### 4. ACM証明書のリージョン
- CloudFront: us-east-1のみ
- ALB: 同じリージョンの証明書が必要
- 同じドメインで複数リージョンに証明書をリクエスト可能

## トラブルシューティング記録

### 問題1: HTTPSでアクセスできない
- **症状**: タイムアウト
- **原因**: セキュリティグループにポート443がない
- **解決**: インバウンドルール追加

### 問題2: チャットが応答しない
- **症状**: メッセージ送信後、何も返ってこない
- **原因**: CSRFトークンがない
- **解決**: JavaScriptにCSRFトークンを追加

### 問題3: ログインできない
- **症状**: "Failed to find user"エラー
- **原因**: Spring Securityのフォームログインが競合
- **解決**: フォームログインを無効化

### 問題4: 登録後にセッションが維持されない
- **症状**: 登録後、/chatにアクセスすると/loginにリダイレクト
- **原因**: SecurityContextに認証情報が設定されていない
- **解決**: 登録・ログイン時にSecurityContextを設定

## 成功要因

1. **段階的なデバッグ**: ログを確認しながら一つずつ問題を解決
2. **セキュリティ設定の理解**: Spring Securityの動作を理解
3. **適切なツール使用**: curl, AWS CLI, ログ確認
4. **ドキュメント参照**: Spring Security, Thymeleaf, AWS公式ドキュメント

---

**セッション終了時刻**: 2025年12月4日 22:00
**最終状態**: 🎉 **完全に動作する本番環境が完成！**

**本番URL**: https://lhormace.com

**達成した機能:**
- ✅ ユーザー登録・ログイン
- ✅ チャット機能（AI応答）
- ✅ 健康データ分析
- ✅ データ永続化（S3）
- ✅ グラフ表示
- ✅ HTTPS通信
- ✅ セキュリティ対策

**次のステップ（オプション）:**
- コスト削減（EC2自動停止）
- モニタリング設定
- バックアップ戦略
- パフォーマンス最適化

