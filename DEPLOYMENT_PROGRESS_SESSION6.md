# デプロイメント進捗 - セッション6
**日付**: 2025年12月4日  
**作業時間**: 約1時間

## 🎯 セッション目標
前セッションで成功したはずのユーザー登録機能が、再度失敗している問題の調査と解決

## 📋 実施した作業

### 1. 問題の確認
**現象**:
- ユーザー登録時に `/login` にリダイレクトされる
- 前セッションでは成功していたが、今回は失敗
- エラーメッセージが表示されない

**初期診断**:
```bash
curl -X POST http://health-chat-env.eba-xaqnxjtp.ap-northeast-1.elasticbeanstalk.com/register \
  -d "username=testuser5&password=password123&email=test5@example.com"
# Result: 302 Redirect to /login
```

### 2. ログレベルの引き上げ
環境変数を設定してDEBUGログを有効化:
```bash
eb setenv LOGGING_LEVEL_COM_HEALTH_CHAT=DEBUG LOGGING_LEVEL_ROOT=INFO
```

**結果**: ログにエラーが出力されない → リクエストがコントローラーに到達していない可能性

### 3. CSRF トークンの問題調査
**発見**: CSRFトークンなしでPOSTしていた可能性

**対応**:
```bash
# CSRFトークンを取得
CSRF_TOKEN=$(curl -s -c cookies.txt http://...​/register | grep '_csrf' | sed -n 's/.*value="\([^"]*\)".*/\1/p')

# CSRFトークン付きでPOST
curl -b cookies.txt -X POST http://...​/register \
  -d "username=testuser8&password=password123&email=test8@example.com&_csrf=$CSRF_TOKEN"
```

**結果**: まだ `/login` にリダイレクトされる

### 4. デバッグログの追加
**AuthController.java** に詳細なログを追加:
```java
@PostMapping("/register")
public String register(...) {
    System.out.println("=== REGISTER REQUEST RECEIVED ===");
    System.out.println("Username: " + username);
    System.out.println("Email: " + email);
    System.out.println("AuthService available: " + (authenticationService != null));
    
    try {
        if (authenticationService != null) {
            System.out.println("Calling registerUser...");
            AuthResult result = authenticationService.registerUser(username, password, email);
            System.out.println("Registration result - Success: " + result.isSuccess());
            System.out.println("Registration result - Error: " + result.getErrorMessage());
            // ... rest of code
        }
    } catch (Exception e) {
        System.out.println("Exception during registration: " + e.getMessage());
        e.printStackTrace();
        // ... error handling
    }
}
```

### 5. テストエンドポイントの追加
AuthServiceが利用可能か確認するためのエンドポイント:
```java
@GetMapping("/test-register")
@ResponseBody
public String testRegister() {
    return "AuthController is working! AuthService: " + (authenticationService != null);
}
```

**テスト結果**:
```bash
curl http://health-chat-env.eba-xaqnxjtp.ap-northeast-1.elasticbeanstalk.com/test-register
# Output: AuthController is working! AuthService: true
```

✅ **確認**: AuthControllerは動作しており、AuthenticationServiceも利用可能

### 6. SecurityConfig の確認
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/login", "/register", "/test-register").permitAll()
    .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
    .anyRequest().authenticated()
)
```

✅ **確認**: `/register` は `.permitAll()` で許可されている

## 🔍 現在の状況

### 動作確認済み
- ✅ アプリケーションは正常に起動
- ✅ AuthController は動作している
- ✅ AuthenticationService は利用可能
- ✅ SecurityConfig で `/register` は許可されている
- ✅ S3バケットへのアクセス権限は設定済み
- ✅ IAMロールに `AmazonS3FullAccess` がアタッチされている

### 未解決の問題
- ❌ POST `/register` リクエストがAuthControllerに到達していない
- ❌ デバッグログ（"=== REGISTER REQUEST RECEIVED ==="）が出力されない
- ❌ ユーザー登録が `/login` にリダイレクトされる
- ❌ エラーメッセージが表示されない

## 🤔 推測される原因

### 可能性1: Spring Security のフィルターチェーン
- Spring Security が POST `/register` をインターセプトしている
- フォームログインの設定が `/register` POST をブロックしている可能性

### 可能性2: セッション管理の問題
- リダイレクトURLに `;HEALTH_CHAT_SESSION=...` が付加されている
- セッション作成に失敗している可能性

### 可能性3: CSRF フィルターの問題
- CSRFトークンは正しく取得・送信しているが、検証で失敗している
- Cookie の SameSite=Strict 設定が影響している可能性

### 可能性4: Nginx のリバースプロキシ設定
- Elastic Beanstalk の Nginx が POST リクエストを正しく転送していない
- リクエストボディが失われている可能性

## 📊 デプロイ情報

### 現在のデプロイ状態
- **環境名**: health-chat-env
- **URL**: http://health-chat-env.eba-xaqnxjtp.ap-northeast-1.elasticbeanstalk.com
- **リージョン**: ap-northeast-1
- **インスタンスタイプ**: t3.small
- **プラットフォーム**: Corretto 17 on Amazon Linux 2
- **最終デプロイ**: 2025-12-04 01:09:59 JST

### 環境変数
```
LOGGING_LEVEL_COM_HEALTH_CHAT=DEBUG
LOGGING_LEVEL_ROOT=INFO
S3_BUCKET_NAME=health-chat-data
AWS_REGION=ap-northeast-1
```

### IAM設定
- **ロール**: aws-elasticbeanstalk-ec2-role
- **ポリシー**: 
  - AWSElasticBeanstalkWebTier
  - AWSElasticBeanstalkWorkerTier
  - AWSElasticBeanstalkMulticontainerDocker
  - AmazonS3FullAccess

## 🔧 次回の調査ポイント

### 優先度: 高
1. **Spring Security のログを有効化**
   ```properties
   logging.level.org.springframework.security=DEBUG
   ```
   これにより、どのフィルターがリクエストを処理しているか確認できる

2. **Nginx のアクセスログを確認**
   ```bash
   eb ssh health-chat-env
   tail -f /var/log/nginx/access.log
   ```
   POST リクエストが Nginx に到達しているか確認

3. **SecurityConfig の formLogin 設定を見直す**
   ```java
   .formLogin(form -> form
       .loginPage("/login")
       .loginProcessingUrl("/login")  // これが /register をインターセプトしていないか
       .defaultSuccessUrl("/chat", true)
       .permitAll()
   )
   ```

4. **CSRF設定を一時的に無効化してテスト**
   ```java
   .csrf(csrf -> csrf.disable())  // テスト用
   ```

### 優先度: 中
5. **リクエストボディのログ出力**
   - Spring の `CommonsRequestLoggingFilter` を追加
   - リクエストボディが正しく届いているか確認

6. **別のHTTPクライアントでテスト**
   - Postman や Insomnia を使用
   - ブラウザから直接テスト

7. **ローカル環境での動作確認**
   ```bash
   java -jar application.jar --spring.profiles.active=production
   ```
   ローカルで同じ設定で動作するか確認

### 優先度: 低
8. **Elastic Beanstalk の .ebextensions 設定確認**
   - Nginx の設定ファイルをカスタマイズしているか確認

9. **CloudWatch Logs の確認**
   - より詳細なログが CloudWatch に出力されていないか確認

## 📝 重要なファイル

### 変更したファイル
1. `src/main/java/com/health/chat/web/AuthController.java`
   - デバッグログ追加
   - テストエンドポイント追加

2. `src/main/java/com/health/chat/config/SecurityConfig.java`
   - テストエンドポイントを permitAll に追加

3. `src/main/resources/application-production.properties`
   - ログレベル設定（環境変数で上書き）

### 確認が必要なファイル
1. `.ebextensions/` ディレクトリ
   - Nginx のカスタム設定があるか確認

2. `src/main/java/com/health/chat/service/JwtAuthenticationService.java`
   - registerUser メソッドの実装

3. `src/main/java/com/health/chat/repository/S3DataRepository.java`
   - saveUserProfile メソッドの実装

## 💡 推奨される次のステップ

### ステップ1: Spring Security のデバッグログを有効化
```bash
eb setenv LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
```

### ステップ2: リアルタイムログ監視
```bash
eb ssh health-chat-env
sudo tail -f /var/log/web.stdout.log
```
別のターミナルで登録リクエストを送信し、ログをリアルタイムで確認

### ステップ3: 最小限の SecurityConfig でテスト
一時的に SecurityConfig を簡素化:
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // 一時的に無効化
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()  // すべて許可
        );
    return http.build();
}
```

### ステップ4: 問題の切り分け
- ローカル環境で動作確認
- AWS環境特有の問題か、コードの問題かを切り分け

## 🔗 関連リソース

### AWS リソース
- **S3 バケット**: health-chat-data
- **Elastic Beanstalk 環境**: health-chat-env
- **EC2 インスタンス**: i-063025b27205592c8
- **セキュリティグループ**: awseb-e-iqhqhqhqhq-stack-AWSEBSecurityGroup-*

### ドキュメント
- Spring Security Reference: https://docs.spring.io/spring-security/reference/
- Elastic Beanstalk Java Platform: https://docs.aws.amazon.com/elasticbeanstalk/latest/dg/java-se-platform.html

## 📌 メモ

### 前セッションとの違い
- 前セッション（Session 5）では、環境を完全に再作成（terminate → create）した後に成功
- 今回は既存環境を使用しているが、同じコードで失敗
- 環境の再作成が必要な可能性あり

### 疑問点
1. なぜ前セッションでは成功したのか？
2. 環境変数の変更だけで動作が変わるのか？
3. EC2インスタンスの再起動が必要なのか？

### 試していないこと
- [ ] 環境の完全な再作成
- [ ] ローカル環境での動作確認
- [ ] Spring Security のデバッグログ
- [ ] Nginx のアクセスログ確認
- [ ] CSRF を無効化してのテスト
- [ ] 別のHTTPクライアントでのテスト

---

**次回セッション開始時のチェックリスト**:
1. [ ] 最新のコードを git pull
2. [ ] Spring Security のデバッグログを有効化
3. [ ] SSH でログをリアルタイム監視しながらテスト
4. [ ] 問題が解決しない場合は環境の再作成を検討
