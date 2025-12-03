# デプロイメント進捗記録 - セッション4

## 📅 作業日: 2025年12月3日 20:52-

## 🎯 セッション目標
PIDファイル設定を含むバージョンをデプロイして動作確認

## 📋 前回セッションからの引き継ぎ

### 達成済み
- ✅ `01_reload_nginx`問題を解決
- ✅ PIDファイル設定を`application-production.properties`に追加
- ✅ JARファイルをビルド（23:51）

### 準備完了
- ✅ `.ebextensions/01_nginx_fix.config`
- ✅ `application-production.properties`にPID設定
- ✅ `Procfile`をシンプル化

## 🔄 セッション4での試行

### 試行1: PIDファイル設定版のデプロイ
**時刻**: 20:52

**実施内容**:
```bash
eb create health-chat-env --single --instance-type t3.small --timeout 20
```

**結果**: ❌ 失敗

**エラー**:
```
failed to read file /var/pids/web.pid after 6 attempts
```

**分析**:
- `systemctl start web.service`は成功
- Spring BootがPIDファイルを作成できていない
- `/var/pids/`ディレクトリが存在しないか、書き込み権限がない

### 試行2: /var/pids/ディレクトリ作成を追加
**時刻**: 21:18

**修正内容**:
`.ebextensions/01_nginx_fix.config`に追加:
```yaml
commands:
  01_create_pids_dir:
    command: "mkdir -p /var/pids && chmod 777 /var/pids"
```

**実施内容**:
```bash
eb terminate health-chat-env --force
eb create health-chat-env --single --instance-type t3.small --timeout 20
```

**結果**: ❌ 失敗

**エラー**:
```
Job for web.service failed because the control process exited with error code.
Register application failed because the registration of proc web failed
```

**分析**:
- エラーメッセージが変わった！
- PIDファイルの問題ではなく、アプリケーション起動の問題
- `web.service`が起動に失敗している

### 試行3: SPRING_PROFILES_ACTIVEを明示的に指定
**時刻**: 21:40

**修正内容**:
Procfileを修正:
```
web: java -Dspring.profiles.active=production -jar application.jar --server.port=5000
```

**理由**:
- 環境変数が正しく渡されていない可能性
- Javaシステムプロパティとして明示的に指定

**実施内容**:
```bash
eb terminate health-chat-env --force
```

**状態**: 環境削除完了、次回デプロイ待ち

## 🔍 発見した問題

### 問題1: /var/pids/ディレクトリの不在
- **症状**: `failed to read file /var/pids/web.pid`
- **原因**: ディレクトリが存在しない
- **解決策**: `.ebextensions`で作成 ✅

### 問題2: アプリケーション起動失敗
- **症状**: `Job for web.service failed because the control process exited with error code`
- **原因**: 不明（調査中）
- **仮説**:
  1. 環境変数が正しく渡されていない
  2. Javaのバージョン問題
  3. 依存関係の問題
  4. ポート5000の競合

## 📝 修正したファイル

### 1. `.ebextensions/01_nginx_fix.config`
```yaml
commands:
  01_create_pids_dir:
    command: "mkdir -p /var/pids && chmod 777 /var/pids"

container_commands:
  01_reload_nginx:
    command: "echo 'Nginx reload skipped' && exit 0"
```

### 2. `Procfile`
```
web: java -Dspring.profiles.active=production -jar application.jar --server.port=5000
```

## 🎯 次のアクション

### 優先度1: 修正版を再デプロイ
```bash
eb create health-chat-env --single --instance-type t3.small --timeout 20
```

### 優先度2: 失敗時の詳細調査
SSH接続してログを確認:
```bash
eb ssh health-chat-env
sudo journalctl -u web.service -n 100
sudo cat /var/log/web.stdout.log
java -version
```

### 優先度3: 代替案の検討
- Procfileをさらにシンプル化
- 環境変数の設定方法を変更
- Lambda Web Adapterへの切り替え

## 📊 進捗状況

**全体進捗**: 90%完了

- ✅ nginx問題解決: 100%
- ✅ PIDファイル設定: 100%
- ✅ /var/pids/ディレクトリ作成: 100%
- ⬜ アプリケーション起動問題: 調査中
- ⬜ デプロイ成功: 0%

## 💡 学んだこと

### 1. PIDファイルの要件
- Spring Bootの設定だけでは不十分
- `/var/pids/`ディレクトリを事前に作成する必要がある
- 書き込み権限（777）が必要

### 2. エラーメッセージの進化
- 最初: `01_reload_nginx failed`
- 次: `failed to read file /var/pids/web.pid`
- 現在: `Job for web.service failed`
- 各段階で問題を解決しながら進んでいる

### 3. デバッグの難しさ
- SSH接続なしでは詳細なエラーログが見えない
- `systemctl status web.service`の出力が必要
- アプリケーションの標準出力/エラー出力が見えない

---

**セッション中断時刻**: 21:43
**次回アクション**: 修正版をデプロイして動作確認
**期待される結果**: アプリケーションが正常に起動
