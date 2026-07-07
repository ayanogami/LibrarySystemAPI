# LibrarySystemAPI
書籍管理システムのバックエンドAPI

## Overview

このリポジトリは、書籍管理システムのバックエンド API を実装するためのものです。

フロントエンドは含めず、書籍と著者を管理する REST API を提供します。

## Requirements

### 実行環境

- JDK 21
- Docker
- SDKMAN

JDK は SDKMAN で管理します。

```bash
sdk env install
sdk env
```

DB の username / password は環境変数で渡します。GitHub Actions では `CI_DB_USERNAME` / `CI_DB_PASSWORD` を Secrets に設定します。

### ローカル起動手順

ローカルでは Docker で PostgreSQL を起動し、同じ username / password を Spring Boot に環境変数で渡します。

Docker Desktop を起動したうえで、以下を実行します。

```bash
POSTGRES_USER=library_system \
POSTGRES_PASSWORD=library_system \
docker-compose up -d postgres
```

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew bootRun
```

テストも同じ DB 接続情報で実行できます。

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew test
```

### PostgreSQL の確認方法

Docker で起動した PostgreSQL には、以下のコマンドで接続できます。

```bash
docker-compose exec postgres psql -U library_system -d library_system_test
```

`psql` に入った後は、以下のコマンドでテーブルやデータを確認できます。

```sql
\dt
\d authors
select * from authors;
select * from flyway_schema_history;
```

`psql` を終了する場合は、以下を実行します。

```sql
\q
```

IntelliJ IDEA の Database ツールウィンドウから確認する場合は、以下の接続情報を使用します。

| Item | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `library_system_test` |
| User | `library_system` |
| Password | `library_system` |

### 技術要件

- Kotlin
- Spring Boot
- jOOQ
- RDB
- Kotest による単体テスト

### データベース構築・アクセス方針

- Flyway を使用して RDB のスキーマを構築・更新する
- Flyway の migration SQL をもとにテーブルを作成する
- jOOQ を使用して RDB のスキーマからコードを自動生成する
- アプリケーションでは jOOQ の生成コードを利用して DB にアクセスする

### 機能要件

- 書籍情報を登録できる
- 書籍情報を更新できる
- 著者情報を登録できる
- 著者情報を更新できる
- 著者に紐づく書籍一覧を取得できる

### 書籍

書籍は以下の情報を持ちます。

- タイトル
- 価格
- 著者
- 出版状況

書籍には以下の制約があります。

- 価格は `0` 以上であること
- 著者は最低 1 人必要
- 著者は複数指定できる
- 出版状況は「未出版」または「出版済み」
- 一度「出版済み」になった書籍は「未出版」に戻せない

### 著者

著者は以下の情報を持ちます。

- 名前
- 生年月日

著者には以下の制約があります。

- 生年月日は現在日以前であること
- 著者は複数の書籍を執筆できる

## API Scope

実装対象の API は以下を想定します。

- 著者作成 API
- 著者更新 API
- 書籍作成 API
- 書籍更新 API
- 著者に紐づく書籍一覧取得 API
