# Implementation Plan

- [x] 1. プロジェクト構造とコアインターフェースのセットアップ
  - Mavenプロジェクトの作成（Java 21）
  - 共通データモデルクラスの定義（HealthData, NutritionInfo, MentalState, TankaPoem等）
  - 各サービスのインターフェース定義
  - jqwikテストフレームワークのセットアップ
  - _Requirements: 9.1, 9.2_

- [x] 2. S3データリポジトリの実装
  - DataRepositoryインターフェースの実装
  - S3クライアントの設定
  - JSON形式でのデータ保存・取得機能
  - 日付ベースのディレクトリ構造の実装
  - _Requirements: 8.1, 8.2_

- [ ]* 2.1 Property 15のテスト実装
  - **Property 15: Data persistence round-trip**
  - **Validates: Requirements 4.5, 6.4, 7.3, 8.1**

- [ ]* 2.2 Property 26のテスト実装
  - **Property 26: Historical data retrieval**
  - **Validates: Requirements 8.4**

- [ ]* 2.3 Property 27のテスト実装
  - **Property 27: Error handling**
  - **Validates: Requirements 8.5**

- [x] 3. 認証サービスの実装
  - AuthenticationServiceインターフェースの実装
  - JWT形式のトークン生成・検証
  - ユーザー情報のS3保存・取得
  - パスワードハッシュ化（BCrypt）
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ]* 3.1 Property 1のテスト実装
  - **Property 1: Credential verification correctness**
  - **Validates: Requirements 1.2**

- [ ]* 3.2 Property 2のテスト実装
  - **Property 2: Token creation on authentication**
  - **Validates: Requirements 1.3**

- [ ]* 3.3 Property 3のテスト実装
  - **Property 3: Token validation on requests**
  - **Validates: Requirements 1.4**

- [ ]* 3.4 Property 4のテスト実装
  - **Property 4: Token invalidation on logout**
  - **Validates: Requirements 1.5**

- [x] 4. メッセージパーサーの実装
  - 体重抽出機能（正規表現ベース）
  - 体脂肪率抽出機能
  - 食事情報抽出機能
  - 運動情報抽出機能
  - 140文字制限のバリデーション
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [ ]* 4.1 Property 5のテスト実装
  - **Property 5: Message length acceptance**
  - **Validates: Requirements 2.1**

- [ ]* 4.2 Property 6のテスト実装
  - **Property 6: Weight extraction**
  - **Validates: Requirements 2.2**

- [ ]* 4.3 Property 7のテスト実装
  - **Property 7: Body fat extraction**
  - **Validates: Requirements 2.3**

- [ ]* 4.4 Property 8のテスト実装
  - **Property 8: Food extraction**
  - **Validates: Requirements 2.4**

- [ ]* 4.5 Property 9のテスト実装
  - **Property 9: Exercise extraction**
  - **Validates: Requirements 2.5**

- [x] 5. 栄養素推定機能の実装
  - NutritionEstimatorインターフェースの実装
  - 食品データベース（JSON）の作成とS3への配置
  - 栄養素推定ロジック
  - 日次集計機能
  - PFCバランス計算
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ]* 5.1 Property 12のテスト実装
  - **Property 12: Nutrition estimation**
  - **Validates: Requirements 4.1**

- [ ]* 5.2 Property 13のテスト実装
  - **Property 13: Daily calorie aggregation**
  - **Validates: Requirements 4.2, 4.4**

- [ ]* 5.3 Property 14のテスト実装
  - **Property 14: PFC balance calculation**
  - **Validates: Requirements 4.3**

- [x] 6. 心理状態分析機能の実装
  - MentalStateAnalyzerインターフェースの実装
  - 感情キーワード辞書の作成
  - 感情トーン判定ロジック（POSITIVE, NEUTRAL, DISCOURAGED）
  - モチベーションレベル計算
  - 対話履歴の考慮
  - _Requirements: 7.1, 7.2, 7.3_

- [ ]* 6.1 Property 22のテスト実装
  - **Property 22: Mental state analysis**
  - **Validates: Requirements 7.1, 7.2**

- [x] 7. 短歌生成機能の実装
  - TankaGeneratorインターフェースの実装
  - 5-7-5-7-7音数制約の実装
  - 健康データと心理状態を組み込んだ短歌生成ロジック
  - 日本語音数カウント機能
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ]* 7.1 Property 18のテスト実装
  - **Property 18: Tanka generation**
  - **Validates: Requirements 6.1**

- [ ]* 7.2 Property 19のテスト実装
  - **Property 19: Tanka syllable structure**
  - **Validates: Requirements 6.2**

- [ ]* 7.3 Property 20のテスト実装
  - **Property 20: Tanka content relevance**
  - **Validates: Requirements 6.3**

- [ ]* 7.4 Property 21のテスト実装
  - **Property 21: Tanka retrieval**
  - **Validates: Requirements 6.5**

- [x] 8. MCPクライアントの実装
  - MCP接続設定
  - 栄養学研究情報の取得機能
  - 睡眠研究情報の取得機能
  - 運動生理学研究情報の取得機能
  - タイムアウト・エラーハンドリング
  - _Requirements: 5.1, 5.2, 5.3_

- [ ]* 8.1 Property 16のテスト実装
  - **Property 16: MCP research reference**
  - **Validates: Requirements 5.1, 5.2, 5.3**

- [x] 9. 健康アドバイザーAIの実装
  - HealthAdvisorAIインターフェースの実装
  - MCP研究情報を活用したアドバイス生成
  - 心理状態に応じたトーン調整（励まし/サポート）
  - アクショナブルな推奨事項の生成
  - 相談内容の分類と対応
  - _Requirements: 5.4, 5.5, 7.4, 7.5, 11.2, 11.3, 11.4, 11.5_

- [ ]* 9.1 Property 17のテスト実装
  - **Property 17: Advice generation**
  - **Validates: Requirements 5.4, 5.5**

- [ ]* 9.2 Property 23のテスト実装
  - **Property 23: Advice tone adaptation for positive state**
  - **Validates: Requirements 7.4**

- [ ]* 9.3 Property 24のテスト実装
  - **Property 24: Advice tone adaptation for discouraged state**
  - **Validates: Requirements 7.5**

- [ ]* 9.4 Property 29のテスト実装
  - **Property 29: Consultation categorization**
  - **Validates: Requirements 11.2**

- [ ]* 9.5 Property 30のテスト実装
  - **Property 30: Empathetic response**
  - **Validates: Requirements 11.3**

- [ ]* 9.6 Property 31のテスト実装
  - **Property 31: Context incorporation in advice**
  - **Validates: Requirements 11.4**

- [ ]* 9.7 Property 32のテスト実装
  - **Property 32: Consultation continuity**
  - **Validates: Requirements 11.5**

- [x] 10. チェックポイント - すべてのテストが通ることを確認
  - すべてのテストが通ることを確認し、問題があればユーザーに質問する

- [x] 11. AWS Lambda関数の実装
  - 認証Lambda（AuthHandler）の実装
  - 対話処理Lambda（ChatHandler）の実装
  - 分析Lambda（AnalysisHandler）の実装
  - Lambda用のハンドラークラス作成
  - 環境変数の設定
  - _Requirements: 10.1, 10.2, 10.4_

- [ ]* 11.1 Property 28のテスト実装
  - **Property 28: Free-form comment acceptance**
  - **Validates: Requirements 11.1**

- [x] 12. AWS CDKインフラストラクチャの実装
  - CDKプロジェクトのセットアップ
  - Lambda関数の定義
  - API Gatewayの定義とルーティング設定
  - S3バケットの定義とライフサイクルポリシー
  - IAM権限の設定
  - _Requirements: 10.2, 10.5, 12.1, 12.3_

- [x] 13. SpringBoot Webアプリケーションの実装
  - SpringBootプロジェクトのセットアップ
  - ログインページの実装
  - チャットインターフェースの実装
  - API Gatewayへのリクエスト送信
  - セッション管理
  - _Requirements: 1.1, 2.1_

- [x] 14. グラフ表示機能の実装
  - Chart.jsまたはJFreeChartの選択と統合
  - 体重・体脂肪率のグラフ生成
  - 時間範囲選択機能（1ヶ月、3ヶ月、6ヶ月）
  - データ取得とグラフレンダリング
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ]* 14.1 Property 10のテスト実装
  - **Property 10: Graph generation**
  - **Validates: Requirements 3.1**

- [ ]* 14.2 Property 11のテスト実装
  - **Property 11: Time range filtering**
  - **Validates: Requirements 3.2, 3.3, 3.4**

- [x] 15. エラーハンドリングとロギングの実装
  - 認証エラーハンドリング
  - 入力バリデーションエラーハンドリング
  - データアクセスエラーハンドリング（リトライロジック）
  - 外部サービスエラーハンドリング
  - CloudWatch Logsへのロギング
  - _Requirements: 8.5_

- [ ]* 15.1 Property 25のテスト実装
  - **Property 25: Data consistency**
  - **Validates: Requirements 8.3**

- [x] 16. 最終チェックポイント - すべてのテストが通ることを確認
  - すべてのテストが通ることを確認し、問題があればユーザーに質問する
