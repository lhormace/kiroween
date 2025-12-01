# EB CLI インストールガイド

## macOSでのインストール

### 方法1: Homebrew（推奨）

```bash
brew install awsebcli
```

### 方法2: pip

```bash
pip3 install awsebcli --upgrade --user
```

インストール後、パスを追加：

```bash
echo 'export PATH="$HOME/Library/Python/3.x/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

（`3.x`は実際のPythonバージョンに置き換えてください）

## インストール確認

```bash
eb --version
```

以下のような出力が表示されればOK：
```
EB CLI 3.20.x (Python 3.x.x)
```

## トラブルシューティング

### コマンドが見つからない場合

```bash
# pipでインストールした場合のパス確認
python3 -m site --user-base

# 出力例: /Users/username/Library/Python/3.11
# この場合、以下をパスに追加：
export PATH="/Users/username/Library/Python/3.11/bin:$PATH"
```

### 権限エラーが出る場合

```bash
# --userオプションを使用
pip3 install awsebcli --upgrade --user
```

## インストール後の次のステップ

EB CLIがインストールできたら、デプロイスクリプトを実行：

```bash
./deploy-eb.sh
```

または手動でデプロイ：

```bash
# 1. ビルド
mvn clean package -DskipTests

# 2. EB初期化
eb init health-chat-advisor --platform "Corretto 17" --region ap-northeast-1

# 3. 環境作成
eb create health-chat-env --single --instance-type t3.small

# 4. デプロイ
eb deploy health-chat-env
```
