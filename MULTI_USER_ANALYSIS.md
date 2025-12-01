# マルチユーザー対応分析レポート

## 実施日時
2025年12月1日

## 結論
✅ **基本的なマルチユーザー対応は実装済み**
⚠️ **いくつかの改善点あり**

---

## ✅ 実装済みの機能

### 1. ユーザー識別とデータ分離
**実装状況**: ✅ 完全対応

#### セッション管理
```java
// AuthController.java
session.setAttribute("token", result.getToken());
session.setAttribute("userId", result.getUserId());
```

各ユーザーのセッションに個別の`userId`と`token`を保存。

#### データ保存時のユーザー分離
```java
// ChatController.java
String userId = (String) session.getAttribute("userId");
dataRepository.saveHealthData(userId, healthData);
dataRepository.saveNutritionInfo(userId, healthData.getDate(), nutritionInfo);
dataRepository.saveMentalState(userId, healthData.getDate(), mentalState);
dataRepository.saveTanka(userId, tanka);
```

全てのデータ保存操作で`userId`を使用してユーザーを識別。

---

### 2. ファイルシステムでのユーザー分離
**実装状況**: ✅ 完全対応

#### LocalFileDataRepository
```
data/
└── users/
    ├── user_<uuid1>/
    │   ├── profile.json
    │   ├── health/
    │   │   └── 2025/
    │   │       └── 12/
    │   │           └── 2025-12-01.json
    │   ├── nutrition/
    │   ├── mental/
    │   └── tanka/
    └── user_<uuid2>/
        ├── profile.json
        ├── health/
        └── ...
```

各ユーザーのデータは完全に分離されたディレクトリに保存。

#### S3DataRepository
```
s3://bucket/
└── users/
    ├── user_<uuid1>/
    │   ├── profile.json
    │   ├── health/2025/12/...
    │   └── ...
    └── user_<uuid2>/
        └── ...
```

S3でも同様にユーザーごとにプレフィックスで分離。

---

### 3. 認証とアクセス制御
**実装状況**: ✅ 基本対応済み

#### セッションベースの認証
```java
// ChatController.java
String token = (String) session.getAttribute("token");
String userId = (String) session.getAttribute("userId");

if (token == null || userId == null) {
    ChatResponse errorResponse = new ChatResponse();
    errorResponse.setResponseText("セッションが無効です。再度ログインしてください。");
    return errorResponse;
}
```

各リクエストでセッションを確認し、未認証ユーザーを拒否。

#### Spring Securityによる保護
```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
    .anyRequest().authenticated()
)
```

認証が必要なエンドポイントを保護。

---

### 4. ユーザープロファイル管理
**実装状況**: ✅ 完全対応

#### ユーザー登録
```java
// JwtAuthenticationService.java
String userId = "user_" + java.util.UUID.randomUUID().toString();
UserProfile newProfile = new UserProfile(
    userId,
    trimmedUsername,
    passwordHash,
    email,
    LocalDateTime.now(),
    LocalDateTime.now()
);
dataRepository.saveUserProfile(newProfile);
```

各ユーザーに一意のUUIDベースのIDを割り当て。

#### ユーザー検索
```java
// DataRepository implementations
UserProfile getUserProfileByUsername(String username);
UserProfile getUserProfile(String userId);
```

ユーザー名またはIDでプロファイルを取得可能。

---

## ⚠️ 改善が必要な点

### 1. 🟡 トークン検証の不足
**問題**: セッションの`token`を取得するが、実際には検証していない

**現状**:
```java
String token = (String) session.getAttribute("token");
if (token == null || userId == null) {
    // エラー
}
// トークンの検証なし！
```

**推奨修正**:
```java
String token = (String) session.getAttribute("token");
String userId = (String) session.getAttribute("userId");

if (token == null || userId == null) {
    return errorResponse("セッションが無効です");
}

// トークンを検証
if (authenticationService != null && !authenticationService.validateToken(token)) {
    session.invalidate();
    return errorResponse("トークンが無効です。再度ログインしてください");
}

// トークンからuserIdを取得して、セッションのuserIdと一致するか確認
String tokenUserId = authenticationService.getUserIdFromToken(token);
if (!userId.equals(tokenUserId)) {
    session.invalidate();
    return errorResponse("認証エラー");
}
```

**影響**: セッションハイジャック攻撃に脆弱

---

### 2. 🟡 クロスユーザーアクセスの防止が不完全
**問題**: URLパラメータでuserIdを指定できる場合、他のユーザーのデータにアクセスできる可能性

**現状**: 全てのAPIがセッションの`userId`を使用（良い）
```java
String userId = (String) session.getAttribute("userId");
dataRepository.getHealthDataByDateRange(userId, startDate, endDate);
```

**潜在的リスク**: 
- 将来的にURLパラメータで`userId`を受け取るAPIを追加した場合
- セッションの`userId`との一致確認が必要

**推奨パターン**:
```java
@GetMapping("/api/user/{userId}/data")
public ResponseEntity<?> getUserData(@PathVariable String userId, HttpSession session) {
    String sessionUserId = (String) session.getAttribute("userId");
    
    // セッションのuserIdと一致するか確認
    if (!userId.equals(sessionUserId)) {
        return ResponseEntity.status(403).body("Access denied");
    }
    
    // データ取得
    return ResponseEntity.ok(dataRepository.getHealthData(userId));
}
```

---

### 3. 🟡 同時ログインセッション管理
**問題**: 同じユーザーが複数デバイスからログインした場合の動作が不明確

**現状**:
```java
// SecurityConfig.java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
    .maximumSessions(1)
    .maxSessionsPreventsLogin(false)  // 新しいログインで古いセッションを無効化
)
```

**動作**: 
- ✅ 最大1セッションに制限
- ✅ 新しいログインで古いセッションを無効化
- ⚠️ ただし、JWTトークンの無効化リストとの連携なし

**推奨改善**:
```java
// ログイン時に古いトークンを無効化
@PostMapping("/login")
public String login(...) {
    AuthResult result = authenticationService.authenticate(username, password);
    
    if (result.isSuccess()) {
        // 古いセッションのトークンを無効化
        String oldToken = (String) session.getAttribute("token");
        if (oldToken != null) {
            authenticationService.invalidateToken(oldToken);
        }
        
        session.setAttribute("token", result.getToken());
        session.setAttribute("userId", result.getUserId());
        return "redirect:/chat";
    }
}
```

---

### 4. 🟢 データ取得時のユーザー確認（実装済み）
**状況**: ✅ 適切に実装されている

```java
// ChatController.java - getGraphData()
String userId = (String) session.getAttribute("userId");

if (userId == null) {
    return ResponseEntity.status(401).body("{\"error\": \"Unauthorized\"}");
}

List<HealthData> healthDataList = dataRepository.getHealthDataByDateRange(userId, startDate, endDate);
```

全てのデータ取得APIでセッションの`userId`を使用。

---

### 5. 🟡 監査ログの不足
**問題**: ユーザーのアクション履歴が記録されていない

**推奨実装**:
```java
@Component
public class AuditLogger {
    private static final Logger LOGGER = Logger.getLogger(AuditLogger.class.getName());
    
    public void logUserAction(String userId, String action, String details) {
        LOGGER.log(Level.INFO, String.format(
            "AUDIT: userId=%s, action=%s, details=%s, timestamp=%s",
            userId, action, details, LocalDateTime.now()
        ));
    }
}

// 使用例
auditLogger.logUserAction(userId, "LOGIN", "Successful login");
auditLogger.logUserAction(userId, "DATA_ACCESS", "Retrieved health data for date range");
auditLogger.logUserAction(userId, "DATA_SAVE", "Saved health data");
```

---

## 📊 マルチユーザー対応スコア

### 総合評価: 85/100

| 項目 | スコア | 状態 |
|------|--------|------|
| ユーザー識別 | 95/100 | ✅ UUID使用、適切 |
| データ分離 | 100/100 | ✅ 完全分離 |
| 認証 | 80/100 | ⚠️ トークン検証不足 |
| アクセス制御 | 85/100 | ⚠️ 改善の余地あり |
| セッション管理 | 80/100 | ⚠️ トークン連携不足 |
| 監査ログ | 60/100 | ⚠️ 未実装 |

---

## 🔧 推奨改善リスト

### 優先度: High
1. **トークン検証の追加** - 全APIエンドポイントでトークンを検証
2. **セッション-トークン連携** - ログイン時に古いトークンを無効化

### 優先度: Medium
3. **監査ログの実装** - ユーザーアクションの記録
4. **アクセス制御の強化** - URLパラメータでのuserIdアクセス防止パターン

### 優先度: Low
5. **ユーザー管理機能** - アカウント削除、データエクスポート等

---

## 🧪 マルチユーザーテストの推奨

### 実施すべきテスト

1. **同時ログインテスト**
   - 同じユーザーが2つのブラウザからログイン
   - 古いセッションが無効化されることを確認

2. **データ分離テスト**
   - ユーザーAとユーザーBが同時にデータを保存
   - 各ユーザーが自分のデータのみ取得できることを確認

3. **セッションハイジャックテスト**
   - 他のユーザーのセッションIDを使用してアクセス試行
   - 拒否されることを確認

4. **トークン無効化テスト**
   - ログアウト後にトークンが無効化されることを確認
   - 無効化されたトークンでアクセスできないことを確認

---

## まとめ

### ✅ 良い点
- ユーザーIDベースのデータ分離が適切に実装されている
- ファイルシステム/S3での物理的な分離が実現されている
- UUIDベースのユーザーID生成で衝突リスクなし
- Spring Securityによる基本的な保護

### ⚠️ 改善点
- トークン検証の追加が必要
- セッション管理とトークン無効化の連携強化
- 監査ログの実装

### 結論
**現状でも基本的なマルチユーザー運用は可能**ですが、セキュリティを強化するために上記の改善を実施することを強く推奨します。特に、トークン検証の追加は早急に対応すべきです。
