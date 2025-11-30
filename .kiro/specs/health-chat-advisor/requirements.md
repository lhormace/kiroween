# Requirements Document

## Introduction

健康管理チャットアドバイザーは、ユーザーが気軽に健康情報を入力し、AIとの対話を通じて健康管理を行うシステムです。従来の硬直的な健康管理システムとは異なり、SNSに投稿するような感覚で体重・体脂肪・食事・運動などの情報を入力でき、システムが自動的に栄養素を推定し、パーソナライズされたアドバイスを提供します。

## Glossary

- **System**: 健康管理チャットアドバイザーシステム全体
- **User**: システムを利用する個人ユーザー
- **Chat Interface**: ユーザーとシステムが対話するインターフェース
- **Health Data**: 体重、体脂肪率、食事内容、運動内容などの健康関連情報
- **Nutrition Estimator**: 食事の文章記述から栄養素を推定するコンポーネント
- **PFC Balance**: タンパク質（Protein）、脂質（Fat）、炭水化物（Carbohydrate）のバランス
- **Health Advisor AI**: MCP経由で最新の栄養学・睡眠研究・運動生理学の情報を参照するAIコンポーネント
- **Mental State Analyzer**: 対話内容から心理状態を推定するエージェント
- **Server Database**: S3で管理されるサーバ側データベース
- **Authentication Service**: ユーザー認証を管理するサービス

## Requirements

### Requirement 1

**User Story:** ユーザーとして、自分のデータを安全に管理したいので、ログイン認証機能が欲しい

#### Acceptance Criteria

1. WHEN User accesses the System THEN THE Authentication Service SHALL require valid credentials
2. WHEN User provides username and password THEN THE System SHALL verify the credentials against stored user records
3. WHEN authentication succeeds THEN THE System SHALL create a session token for the User
4. WHEN User makes subsequent requests THEN THE System SHALL validate the session token
5. WHEN User logs out THEN THE System SHALL invalidate the session token

### Requirement 2

**User Story:** ユーザーとして、気軽に健康情報を入力したいので、短いメッセージで対話できるシステムが欲しい

#### Acceptance Criteria

1. WHEN User submits a message THEN THE System SHALL accept text input of up to 140 characters
2. WHEN User inputs health data THEN THE System SHALL parse and extract body weight values from the message
3. WHEN User inputs health data THEN THE System SHALL parse and extract body fat percentage values from the message
4. WHEN User inputs daily activities THEN THE System SHALL parse and extract food consumption information from the message
5. WHEN User inputs daily activities THEN THE System SHALL parse and extract exercise information from the message

### Requirement 3

**User Story:** ユーザーとして、入力した体重と体脂肪率の推移を視覚的に確認したいので、グラフ表示機能が欲しい

#### Acceptance Criteria

1. WHEN User requests weight and body fat visualization THEN THE System SHALL generate a graph displaying the historical data
2. WHEN User selects a one-month time range THEN THE System SHALL display data for the past 30 days
3. WHEN User selects a three-month time range THEN THE System SHALL display data for the past 90 days
4. WHEN User selects a six-month time range THEN THE System SHALL display data for the past 180 days
5. WHEN historical data is retrieved THEN THE System SHALL render the graph using JFreeChart or Chart.js

### Requirement 4

**User Story:** ユーザーとして、食事内容から自動的に栄養情報を知りたいので、栄養素推定機能が欲しい

#### Acceptance Criteria

1. WHEN User inputs food consumption text THEN THE Nutrition Estimator SHALL analyze the text and estimate nutritional content
2. WHEN nutritional content is estimated THEN THE System SHALL calculate total daily calorie intake
3. WHEN nutritional content is estimated THEN THE System SHALL calculate PFC balance for the day
4. WHEN multiple food entries exist for a day THEN THE System SHALL aggregate all entries to compute daily totals
5. WHEN nutritional data is calculated THEN THE System SHALL persist the results to the Server Database

### Requirement 5

**User Story:** ユーザーとして、科学的根拠に基づいた健康アドバイスが欲しいので、最新の研究情報を参照するAIアドバイザーが欲しい

#### Acceptance Criteria

1. WHEN User inputs health data THEN THE Health Advisor AI SHALL reference latest nutrition research via MCP
2. WHEN User inputs health data THEN THE Health Advisor AI SHALL reference latest sleep research via MCP
3. WHEN User inputs health data THEN THE Health Advisor AI SHALL reference latest exercise physiology research via MCP
4. WHEN health analysis is complete THEN THE System SHALL generate personalized health advice based on User data and research
5. WHEN advice is generated THEN THE System SHALL present actionable recommendations to the User

### Requirement 6

**User Story:** ユーザーとして、日々の記録を振り返りたいので、対話内容を短歌として記録する機能が欲しい

#### Acceptance Criteria

1. WHEN User completes a daily health input session THEN THE System SHALL generate a tanka poem summarizing the day
2. WHEN a tanka is generated THEN THE System SHALL follow traditional 5-7-5-7-7 syllable structure
3. WHEN a tanka is generated THEN THE System SHALL incorporate key health events from the day
4. WHEN a tanka is created THEN THE System SHALL persist it to the Server Database as a diary entry
5. WHEN User requests past diary entries THEN THE System SHALL retrieve and display historical tanka records

### Requirement 7

**User Story:** ユーザーとして、より効果的なアドバイスを受けたいので、心理状態を分析してコンサルティングに活用する機能が欲しい

#### Acceptance Criteria

1. WHEN User engages in conversation THEN THE Mental State Analyzer SHALL analyze the dialogue content
2. WHEN dialogue is analyzed THEN THE System SHALL identify emotional indicators such as positivity, motivation, or discouragement
3. WHEN emotional state is identified THEN THE System SHALL persist the psychological analysis to the Server Database
4. WHEN User shows positive and motivated patterns THEN THE System SHALL provide encouraging and challenging advice
5. WHEN User shows discouraged or negative patterns THEN THE System SHALL provide supportive and gentle guidance in consultation responses

### Requirement 8

**User Story:** 開発者として、データを安全に保存したいので、サーバ側データベースで管理される仕組みが欲しい

#### Acceptance Criteria

1. WHEN health data is submitted THEN THE System SHALL persist it to S3 as the Server Database
2. WHEN data is stored THEN THE System SHALL organize files in a structured format for efficient retrieval
3. WHEN data operations occur THEN THE System SHALL maintain data consistency across related records
4. WHEN User requests historical data THEN THE System SHALL retrieve it from S3 efficiently
5. WHEN database operations fail THEN THE System SHALL log the error and handle it gracefully

### Requirement 9

**User Story:** 開発者として、初期開発段階で機能を迅速に追加したいので、拡張性の高いアーキテクチャが欲しい

#### Acceptance Criteria

1. WHEN developers add new features THEN THE System SHALL support extension without requiring major refactoring
2. WHEN backend logic is modified THEN THE System SHALL maintain clear separation between business logic and data access layers
3. WHEN dialogue processing is updated THEN THE System SHALL isolate changes to AWS Lambda functions
4. WHEN API endpoints are added or modified THEN THE System SHALL use API Gateway for consistent interface management
5. WHEN system components interact THEN THE System SHALL use well-defined interfaces to enable independent development

### Requirement 10

**User Story:** システム管理者として、対話処理をスケーラブルに実行したいので、AWS Lambda上で動作する仕組みが欲しい

#### Acceptance Criteria

1. WHEN User initiates a conversation THEN THE System SHALL invoke AWS Lambda functions to process dialogue logic
2. WHEN Lambda functions are invoked THEN THE System SHALL use API Gateway to route requests appropriately
3. WHEN dialogue processing completes THEN THE System SHALL return responses within acceptable latency limits
4. WHEN Lambda functions execute THEN THE System SHALL use Java 21 runtime environment
5. WHEN API requests are received THEN THE System SHALL authenticate and authorize using AWS IAM policies

### Requirement 11

**User Story:** ユーザーとして、悩みや困りごとを相談したいので、対話の中で自由にコメントできる機能が欲しい

#### Acceptance Criteria

1. WHEN User inputs free-form comments THEN THE System SHALL accept and store the text without strict formatting requirements
2. WHEN User shares concerns or worries THEN THE System SHALL categorize the input as consultation content
3. WHEN consultation content is received THEN THE System SHALL provide empathetic responses
4. WHEN User discusses challenges THEN THE System SHALL incorporate the context into personalized advice generation
5. WHEN consultation history exists THEN THE System SHALL reference past discussions to provide continuity in support

### Requirement 12

**User Story:** システム管理者として、運用コストを抑えたいので、費用効率の高いアーキテクチャが欲しい

#### Acceptance Criteria

1. WHEN AWS services are selected THEN THE System SHALL prioritize serverless and pay-per-use services
2. WHEN Lambda functions execute THEN THE System SHALL optimize execution time to minimize compute costs
3. WHEN data is stored in S3 THEN THE System SHALL use appropriate storage classes to reduce storage costs
4. WHEN API requests are processed THEN THE System SHALL implement efficient caching strategies to reduce redundant processing
5. WHEN system resources are allocated THEN THE System SHALL scale automatically based on actual usage to avoid over-provisioning
