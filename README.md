# LibrarySystemAPI

書籍管理システムのバックエンドAPIです。書籍、著者、書籍と著者の関連を管理するREST APIを提供します。

## API仕様

SwaggerのようにIFを一覧できるOpenAPI定義を用意しています。

- OpenAPI定義: [docs/openapi.yaml](docs/openapi.yaml)
- Swagger Editorなどに `docs/openapi.yaml` を読み込むと、エンドポイント、request / response、エラーレスポンスを確認できます。

実装済みAPIは以下です。

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/authors` | 著者を作成する |
| `PATCH` | `/authors/{authorId}` | 著者を更新する |
| `GET` | `/books?authorId={authorId}` | 指定した著者に紐づく書籍一覧を取得する |
| `POST` | `/books` | 書籍を作成する |
| `PATCH` | `/books/{bookId}` | 書籍を更新する |

手元確認用のHTTP Clientファイルもあります。

- [http/authors.http](http/authors.http)
- [http/books.http](http/books.http)

## Requirements

- JDK 21
- SDKMAN
- Docker / Docker Compose

JDKはSDKMANで管理します。

```bash
sdk env install
sdk env
```

## DB起動

Docker Desktopを起動したうえで、PostgreSQLを起動します。

```bash
POSTGRES_USER=library_system \
POSTGRES_PASSWORD=library_system \
docker compose up -d postgres
```

PostgreSQLに接続して確認する場合は以下です。

```bash
docker compose exec postgres psql -U library_system -d library_system_test
```

よく使う確認コマンドです。

```sql
\dt
\d authors
\d books
\d book_authors
select * from authors;
select * from books;
select * from book_authors;
select * from flyway_schema_history;
```

終了する場合は以下です。

```sql
\q
```

IntelliJ IDEAのDatabaseツールウィンドウから接続する場合は以下を使います。

| Item | Value |
| --- | --- |
| Host | `localhost` |
| Port | `5432` |
| Database | `library_system_test` |
| User | `library_system` |
| Password | `library_system` |

## アプリ起動

DB起動後、Spring Bootアプリケーションを起動します。

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew bootRun
```

起動後のベースURLは以下です。

```text
http://localhost:8080
```

## テスト

DB起動後、テストを実行します。

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew test
```

ビルド全体を確認する場合は以下です。

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew build
```

カバレッジレポートを出す場合は以下です。

```bash
SPRING_DATASOURCE_USERNAME=library_system \
SPRING_DATASOURCE_PASSWORD=library_system \
./gradlew test jacocoTestReport
```

HTMLレポートは以下に出力されます。

```text
build/reports/jacoco/test/html/index.html
```

JaCoCoではjOOQ生成コードを除外し、アプリケーション本体のカバレッジを確認します。

## 代表的なAPIリクエスト

### 著者を作成する

```http
POST http://localhost:8080/authors
Content-Type: application/json

{
  "name": "夏目漱石",
  "birthDate": "1867-02-09"
}
```

### 著者を更新する

```http
PATCH http://localhost:8080/authors/1
Content-Type: application/json

{
  "name": "夏目 金之助"
}
```

### 書籍を作成する

```http
POST http://localhost:8080/books
Content-Type: application/json

{
  "title": "吾輩は猫である",
  "price": 1200,
  "authorIds": [1],
  "publicationStatus": "PUBLISHED"
}
```

### 書籍を更新する

```http
PATCH http://localhost:8080/books/1
Content-Type: application/json

{
  "price": 1500,
  "authorIds": [1, 2]
}
```

### 著者に紐づく書籍一覧を取得する

```http
GET http://localhost:8080/books?authorId=1
```

## エラーレスポンス

APIエラーは共通形式で返します。

```json
{
  "code": "VALIDATION_ERROR",
  "message": "validation failed",
  "details": [
    {
      "field": "name",
      "message": "must not be blank"
    }
  ]
}
```

主な `code` は以下です。

| Code | Description |
| --- | --- |
| `VALIDATION_ERROR` | request bodyのvalidation error |
| `BAD_REQUEST` | 不正なrequest、業務ルール違反 |
| `NOT_FOUND` | 指定したリソースが存在しない |

## 技術構成

- Kotlin
- Spring Boot
- PostgreSQL
- Flyway
- jOOQ
- Kotest
- JaCoCo

## DB構築とアクセス方針

- FlywayでRDBのスキーマを構築、更新する
- Flyway migration SQLをもとにテーブルを作成する
- jOOQでRDBスキーマからコードを自動生成する
- アプリケーションではjOOQ生成コードを利用してDBにアクセスする

## ドメインルール

### 書籍

書籍は以下の情報を持ちます。

- タイトル
- 価格
- 著者
- 出版状況

制約は以下です。

- 価格は `0` 以上
- 著者は最低1人必要
- 著者は複数指定できる
- 出版状況は `UNPUBLISHED` または `PUBLISHED`
- 一度 `PUBLISHED` になった書籍は `UNPUBLISHED` に戻せない

### 著者

著者は以下の情報を持ちます。

- 名前
- 生年月日

制約は以下です。

- 名前は空にできない
- 生年月日は現在日以前
- 著者は複数の書籍を執筆できる
