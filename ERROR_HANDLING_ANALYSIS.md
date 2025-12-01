# エラーハンドリング分析レポート

## 実施日時
2025年12月1日

## 結論
⚠️ **部分的に実装済みだが、一貫性と改善が必要**

---

## ✅ 実装済みの機能

### 1. ErrorHandlerユーティリティクラス
**実装状況**: ✅ Lambda関数で使用

#### 機能
```java
public class ErrorHandler {
    // エラータイプの分類
    public enum ErrorType {
        AUTHENTICATION_ERROR,
        VALIDATION_ERROR,
        DATA_ACCESS_ERROR,
        EXTERNAL_SERVICE_ERROR,
        INTERNAL_ERROR
    }
    
    // 各種エラーハンドラー
    - handleAuthenticationError()
    - handleValidationError()
    - handleDataAccessError()
    - handleExternalServiceError()
    - handleInternalError()
    - isRetryable() // リトライ可能判定
}
```

**使用箇所**: Lambda関数（ChatHandler, AuthHandler, AnalysisHandler）

**良い点**:
- エラーの分類が明確
- 一貫したエラーレスポンス形式
- ログ出力の統一
- リトライ可能性の判定

---

### 2. カスタム例外クラス
**実装状況**: ✅ 最小限の実装

```java
public class MCPException extends Exception {
    public MCPException(String message) {
        super(message);
    }
    
    public MCPException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**使用箇所**: MCP（Model Context Protocol）サービス

---

### 3. Repository層のエラーハンドリング
**実装状況**: ✅ 基本的な実装

#### LocalFileDataRepository
```java
try {
    // ファイル操作
    objectMapper.writeValue(filePath.toFile(), data);
    LOGGER.info("Saved health data for user: " + userId);
} catch (IOException e) {
    LOGGER.log(Level.SEVERE, "Failed to save health data", e);
    throw new RuntimeException("Failed to save health data", e);
}
```

#### S3DataRepository
```java
// リトライロジック付き
int attempts = 0;
while (attempts < MAX_RETRIES) {
    try {
        s3Client.putObject(putRequest, RequestBody.fromString(jsonContent));
        return;
    } catch (S3Exception e) {
        attempts++;
        if (attempts >= MAX_RETRIES) {
            break;
        }
        Thread.sleep(1000 * attempts); // Exponential backoff
    }
}
throw new RuntimeException("Failed to save data to S3", lastException);
```

**良い点**:
- S3操作にリトライロジック実装
- エクスポネンシャルバックオフ
- 詳細なログ出力

---

## ⚠️ 問題点と改善が必要な箇所

### 1. 🔴 Webコントローラーのエラーハンドリングが不統一
**問題**: ChatControllerとAuthControllerでエラー処理が異なる

#### ChatController
```java
try {
    // 処理
} catch (Exception e) {
    System.out.println("Error processing message: " + e.getMessage());
    e.printStackTrace();  // ❌ セキュリティリスク
    ChatResponse errorResponse = new ChatResponse();
    errorResponse.setResponseText("メッセージの処理中にエラーが発生しました: " + e.getMessage());
    return errorResponse;
}
```

**問題点**:
- `System.out.println`使用（ログ管理不適切）
- `printStackTrace()`使用（機密情報漏洩リスク）
- エラーメッセージに例外メッセージを含む（内部情報漏洩）
- エラータイプの分類なし

#### AuthController
```java
try {
    // 処理
} catch (Exception e) {
    model.addAttribute("error", "認証エラーが発生しました");
    return "login";
}
```

**問題点**:
- ログ出力なし
- 例外の詳細が失われる
- デバッグが困難

---

### 2. 🔴 グローバルエラーハンドラーの欠如
**問題**: Spring MVCの`@ControllerAdvice`が未実装

**現状**: 各コントローラーで個別にtry-catchを実装

**推奨実装**:
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionHandler.class.getName());
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        LOGGER.log(Level.SEVERE, "Unhandled exception", e);
        
        ErrorResponse error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            null
        );
        
        return ResponseEntity.status(500).body(error);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthenticationException e) {
        LOGGER.log(Level.WARNING, "Authentication failed", e);
        
        ErrorResponse error = new ErrorResponse(
            "AUTHENTICATION_ERROR",
            "Authentication failed",
            null
        );
        
        return ResponseEntity.status(401).body(error);
    }
    
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException e) {
        LOGGER.log(Level.SEVERE, "Data access error", e);
        
        ErrorResponse error = new ErrorResponse(
            "DATA_ACCESS_ERROR",
            "Failed to access data",
            null
        );
        
        return ResponseEntity.status(500).body(error);
    }
}
```

---

### 3. 🟡 カスタム例外クラスの不足
**問題**: MCPException以外のカスタム例外がない

**推奨追加**:
```java
// 認証関連
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

public class InvalidTokenException extends AuthenticationException {
    public InvalidTokenException(String message) {
        super(message);
    }
}

// データアクセス関連
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class UserNotFoundException extends DataAccessException {
    public UserNotFoundException(String userId) {
        super("User not found: " + userId, null);
    }
}

// バリデーション関連
public class ValidationException extends RuntimeException {
    private final Map<String, String> errors;
    
    public ValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
```

---

### 4. 🟡 エラーレスポンスの標準化不足
**問題**: レスポンス形式が統一されていない

**現状**:
- Lambda: `Map<String, Object>`
- Web: `ChatResponse`または`String`
- 一部: HTTPステータスコードのみ

**推奨実装**:
```java
public class ErrorResponse {
    private String errorCode;
    private String message;
    private String details;
    private LocalDateTime timestamp;
    private String path;
    
    // コンストラクタ、getter/setter
}

// 使用例
@PostMapping("/api/chat")
public ResponseEntity<?> sendMessage(...) {
    try {
        // 処理
        return ResponseEntity.ok(response);
    } catch (ValidationException e) {
        ErrorResponse error = new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid input",
            e.getMessage(),
            LocalDateTime.now(),
            "/api/chat"
        );
        return ResponseEntity.badRequest().body(error);
    }
}
```

---

### 5. 🟡 部分的なエラー処理
**問題**: データ保存失敗時の処理が不完全

```java
// ChatController.java
if (dataRepository != null) {
    try {
        dataRepository.saveHealthData(userId, healthData);
        // ...
    } catch (Exception e) {
        System.out.println("Warning: Failed to save data: " + e.getMessage());
        // ❌ エラーを無視して続行
    }
}
```

**問題点**:
- データ保存失敗をユーザーに通知しない
- 部分的な保存失敗の可能性
- トランザクション管理なし

**推奨対応**:
```java
try {
    // トランザクション開始
    dataRepository.saveHealthData(userId, healthData);
    if (nutritionInfo != null) {
        dataRepository.saveNutritionInfo(userId, healthData.getDate(), nutritionInfo);
    }
    dataRepository.saveMentalState(userId, healthData.getDate(), mentalState);
    dataRepository.saveTanka(userId, tanka);
    // コミット
} catch (Exception e) {
    // ロールバック
    LOGGER.log(Level.SEVERE, "Failed to save data", e);
    throw new DataAccessException("Failed to save health data", e);
}
```

---

### 6. 🟡 ログレベルの不適切な使用
**問題**: ログレベルが一貫していない

**現状**:
- `System.out.println` - ログ管理外
- `LOGGER.info` - 成功時
- `LOGGER.warning` - 警告
- `LOGGER.severe` - エラー

**推奨**:
```java
// DEBUG: 開発時のデバッグ情報
LOGGER.log(Level.FINE, "Processing message: {0}", message);

// INFO: 通常の操作ログ
LOGGER.log(Level.INFO, "User logged in successfully");

// WARNING: 警告（処理は継続）
LOGGER.log(Level.WARNING, "MCP service unavailable, using fallback");

// SEVERE: エラー（処理失敗）
LOGGER.log(Level.SEVERE, "Failed to save data", e);
```

---

### 7. 🟢 Lambda関数のエラーハンドリング（良好）
**実装状況**: ✅ 適切に実装

```java
// ChatHandler.java
try {
    // 処理
    return createResponse(200, response);
} catch (Exception e) {
    Map<String, Object> error = ErrorHandler.handleInternalError("message processing", context, e);
    return createResponse(500, error);
}
```

**良い点**:
- ErrorHandlerを使用
- 一貫したエラーレスポンス
- 適切なHTTPステータスコード
- ログ出力

---

## 📊 エラーハンドリングスコア

### 総合評価: 65/100

| 項目 | スコア | 状態 |
|------|--------|------|
| Lambda関数 | 90/100 | ✅ 良好 |
| Repository層 | 80/100 | ✅ 基本的に良好 |
| Webコントローラー | 40/100 | 🔴 要改善 |
| カスタム例外 | 50/100 | 🟡 不足 |
| グローバルハンドラー | 0/100 | 🔴 未実装 |
| ログ管理 | 60/100 | 🟡 改善必要 |
| エラーレスポンス | 55/100 | 🟡 不統一 |

---

## 🔧 優先度別改善リスト

### 🔴 Critical（即座に対応）
1. **System.out.printlnとprintStackTrace()の削除**
   - セキュリティリスク
   - 既にSECURITY_IMPROVEMENTS.mdで対応済み

2. **エラーメッセージからの内部情報削除**
   ```java
   // ❌ 悪い例
   errorResponse.setResponseText("エラー: " + e.getMessage());
   
   // ✅ 良い例
   errorResponse.setResponseText("処理中にエラーが発生しました");
   LOGGER.log(Level.SEVERE, "Error details", e);
   ```

### 🟡 High（早急に対応）
3. **GlobalExceptionHandlerの実装**
   - `@ControllerAdvice`を使用
   - 全てのコントローラーで統一されたエラー処理

4. **カスタム例外クラスの追加**
   - AuthenticationException
   - DataAccessException
   - ValidationException

5. **ErrorResponseクラスの実装**
   - 統一されたエラーレスポンス形式

### 🟡 Medium（計画的に対応）
6. **データ保存のトランザクション管理**
   - 部分的な保存失敗の防止
   - ロールバック機能

7. **ログ管理の改善**
   - System.out.printlnの完全削除
   - ログレベルの統一
   - 構造化ログの導入

8. **リトライロジックの統一**
   - 全てのデータアクセスにリトライ実装
   - 設定可能なリトライポリシー

---

## 🧪 推奨テスト

### エラーハンドリングテスト
```java
@Test
public void testAuthenticationError() {
    // 認証エラーが適切に処理されることを確認
}

@Test
public void testDataAccessError() {
    // データアクセスエラーが適切に処理されることを確認
}

@Test
public void testValidationError() {
    // バリデーションエラーが適切に処理されることを確認
}

@Test
public void testPartialSaveFailure() {
    // 部分的な保存失敗時のロールバックを確認
}
```

---

## まとめ

### ✅ 良い点
- Lambda関数でErrorHandlerを使用した統一的なエラー処理
- S3DataRepositoryでリトライロジック実装
- エラータイプの分類

### ⚠️ 改善点
- Webコントローラーのエラー処理が不統一
- グローバルエラーハンドラーの欠如
- カスタム例外クラスの不足
- エラーレスポンス形式の不統一
- ログ管理の改善必要

### 結論
Lambda関数では適切なエラーハンドリングが実装されていますが、**Webアプリケーション部分では大幅な改善が必要**です。特に、GlobalExceptionHandlerの実装とSystem.out.printlnの削除は早急に対応すべきです。
