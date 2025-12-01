# Elastic Beanstalk デプロイメント トラブルシューティング

## 現在の問題

Elastic Beanstalk環境が不安定な状態になっており、デプロイが繰り返し失敗しています。

## 推奨される解決策

### オプション1: 環境を削除して再作成（推奨）

```bash
# 1. 現在の環境を削除
eb terminate health-chat-env

# 2. 環境を再作成
eb create health-chat-env --single --instance-type t3.small

# 3. デプロイ
eb deploy health-chat-env
```

### オプション2: 簡易デプロイ方法を試す

Elastic Beanstalkの複雑さを避けて、より単純な方法を試します：

#### A. Lambda Web Adapter（既存のCDKスタック）

```bash
cd cdk
cdk deploy HealthChatAdvisorStack
```

#### B. EC2に直接デプロイ

1. EC2インスタンスを作成
2. JARファイルをアップロード
3. systemdサービスとして実行

## 現在のビルド設定

以下の設定で`application.jar`が正しくビルドされています：

```xml
<build>
    <finalName>application</finalName>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <mainClass>com.health.chat.HealthChatApplication</mainClass>
                <layout>JAR</layout>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## ローカルでの動作確認

```bash
# ビルド
mvn clean package -DskipTests

# ローカルで実行
java -jar target/application.jar

# ブラウザで確認
open http://localhost:8080
```

## 次のステップ

1. **環境を削除して再作成**（最も確実）
2. または **Lambda Web Adapterを使用**（サーバーレス）
3. または **手動でEC2にデプロイ**（シンプル）

どの方法を選びますか？
