package com.ayanogami.library.system.api

import com.ayanogami.library.system.api.jooq.generated.Tables.AUTHORS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOKS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOK_AUTHORS
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class AuthorApiSpec : DescribeSpec({
	extension(SpringExtension())

	lateinit var mockMvc: MockMvc
	lateinit var dsl: DSLContext

	beforeSpec { spec ->
		val authorApiSpec = spec as AuthorApiSpec
		mockMvc = authorApiSpec.mockMvc
		dsl = authorApiSpec.dsl
	}

	beforeTest {
		dsl.truncate(BOOK_AUTHORS, BOOKS, AUTHORS)
			.restartIdentity()
			.cascade()
			.execute()
	}

	fun createAuthor(
		name: String = "夏目漱石",
		birthDate: LocalDate = LocalDate.of(1867, 2, 9),
	): Long = dsl
		.insertInto(AUTHORS)
		.set(AUTHORS.NAME, name)
		.set(AUTHORS.BIRTH_DATE, birthDate)
		.returning(AUTHORS.ID)
		.fetchOne()
			?.get(AUTHORS.ID)
			?: error("Failed to create author for test")

		fun createBook(
			title: String = "吾輩は猫である",
			price: Int = 1200,
			publicationStatus: String = "UNPUBLISHED",
			authorIds: List<Long>,
		): Long {
			val bookId = dsl
				.insertInto(BOOKS)
				.set(BOOKS.TITLE, title)
				.set(BOOKS.PRICE, price)
				.set(BOOKS.PUBLICATION_STATUS, publicationStatus)
				.returning(BOOKS.ID)
				.fetchOne()
				?.get(BOOKS.ID)
				?: error("Failed to create book for test")

			authorIds.forEach { authorId ->
				dsl.insertInto(BOOK_AUTHORS)
					.set(BOOK_AUTHORS.BOOK_ID, bookId)
					.set(BOOK_AUTHORS.AUTHOR_ID, authorId)
					.execute()
			}

			return bookId
		}

		describe("POST /authors") {
			context("リクエストが妥当な場合") {
				it("著者を作成する") {
				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "夏目漱石",
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isCreated)
					.andExpect(jsonPath("$.id").isNumber)
					.andExpect(jsonPath("$.name").value("夏目漱石"))
					.andExpect(jsonPath("$.birthDate").value("1867-02-09"))

				val author = dsl.selectFrom(AUTHORS).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
				author?.get(AUTHORS.BIRTH_DATE).toString() shouldBe "1867-02-09"
			}
		}

		context("生年月日が現在日の場合") {
			it("著者を作成する") {
				val today = LocalDate.now()

				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "今日 生まれ",
							  "birthDate": "$today"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isCreated)
					.andExpect(jsonPath("$.name").value("今日 生まれ"))
					.andExpect(jsonPath("$.birthDate").value(today.toString()))

				val author = dsl.selectFrom(AUTHORS).fetchOne()

				author?.get(AUTHORS.BIRTH_DATE) shouldBe today
			}
		}

		context("著者名が空白の場合") {
			it("400 Bad Request を返す") {
				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "   ",
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
					)
						.andExpect(status().isBadRequest)
						.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
						.andExpect(jsonPath("$.message").value("validation failed"))
						.andExpect(jsonPath("$.details[0].field").value("name"))

					dsl.fetchCount(AUTHORS) shouldBe 0
				}
		}

		context("著者名が未指定の場合") {
			it("400 Bad Request を返す") {
				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(AUTHORS) shouldBe 0
			}
		}

		context("生年月日が未指定の場合") {
			it("400 Bad Request を返す") {
				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "夏目漱石"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(AUTHORS) shouldBe 0
			}
		}

		context("生年月日が現在日より後の場合") {
			it("400 Bad Request を返す") {
				mockMvc.perform(
					post("/authors")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "未来 太郎",
							  "birthDate": "2999-01-01"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(AUTHORS) shouldBe 0
			}
			}
		}

		describe("GET /books?authorId={authorId}") {
			context("著者に書籍が紐づく場合") {
				it("著者情報と書籍一覧を返す") {
					val authorId = createAuthor()
					val otherAuthorId = createAuthor(
						name = "森鴎外",
						birthDate = LocalDate.of(1862, 2, 17),
					)
					val firstBookId = createBook(
						title = "吾輩は猫である",
						price = 1200,
						publicationStatus = "PUBLISHED",
						authorIds = listOf(authorId),
					)
					val secondBookId = createBook(
						title = "坊っちゃん",
						price = 900,
						publicationStatus = "UNPUBLISHED",
						authorIds = listOf(authorId, otherAuthorId),
					)
					createBook(
						title = "舞姫",
						price = 1000,
						publicationStatus = "PUBLISHED",
						authorIds = listOf(otherAuthorId),
					)

					mockMvc.perform(get("/books").param("authorId", authorId.toString()))
						.andExpect(status().isOk)
						.andExpect(jsonPath("$.id").value(authorId))
						.andExpect(jsonPath("$.name").value("夏目漱石"))
						.andExpect(jsonPath("$.birthDate").value("1867-02-09"))
						.andExpect(jsonPath("$.books.length()").value(2))
						.andExpect(jsonPath("$.books[0].id").value(firstBookId))
						.andExpect(jsonPath("$.books[0].title").value("吾輩は猫である"))
						.andExpect(jsonPath("$.books[0].price").value(1200))
						.andExpect(jsonPath("$.books[0].publicationStatus").value("PUBLISHED"))
						.andExpect(jsonPath("$.books[1].id").value(secondBookId))
						.andExpect(jsonPath("$.books[1].title").value("坊っちゃん"))
						.andExpect(jsonPath("$.books[1].price").value(900))
						.andExpect(jsonPath("$.books[1].publicationStatus").value("UNPUBLISHED"))
				}
			}

			context("著者に紐づく書籍がない場合") {
				it("空の書籍一覧を返す") {
					val authorId = createAuthor()

					mockMvc.perform(get("/books").param("authorId", authorId.toString()))
						.andExpect(status().isOk)
						.andExpect(jsonPath("$.id").value(authorId))
						.andExpect(jsonPath("$.name").value("夏目漱石"))
						.andExpect(jsonPath("$.birthDate").value("1867-02-09"))
						.andExpect(jsonPath("$.books.length()").value(0))
				}
			}

			context("著者IDが存在しない場合") {
				it("404 Not Found を返す") {
					mockMvc.perform(get("/books").param("authorId", "999999"))
						.andExpect(status().isNotFound)
				}
			}
		}

		describe("PATCH /authors/{authorId}") {
			context("著者名のみ指定された場合") {
				it("著者名だけを更新する") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "夏目 金之助"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.id").value(authorId))
					.andExpect(jsonPath("$.name").value("夏目 金之助"))
					.andExpect(jsonPath("$.birthDate").value("1867-02-09"))

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目 金之助"
				author?.get(AUTHORS.BIRTH_DATE).toString() shouldBe "1867-02-09"
			}
		}

		context("生年月日のみ指定された場合") {
			it("生年月日だけを更新する") {
				val updatedBirthDate = LocalDate.of(1867, 2, 10)
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "birthDate": "$updatedBirthDate"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.id").value(authorId))
					.andExpect(jsonPath("$.name").value("夏目漱石"))
					.andExpect(jsonPath("$.birthDate").value(updatedBirthDate.toString()))

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
				author?.get(AUTHORS.BIRTH_DATE) shouldBe updatedBirthDate
			}
		}

		context("生年月日が現在日の場合") {
			it("著者を更新する") {
				val authorId = createAuthor()
				val today = LocalDate.now()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "birthDate": "$today"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.id").value(authorId))
					.andExpect(jsonPath("$.name").value("夏目漱石"))
					.andExpect(jsonPath("$.birthDate").value(today.toString()))

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
				author?.get(AUTHORS.BIRTH_DATE) shouldBe today
			}
		}

		context("著者IDが存在しない場合") {
			it("404 Not Found を返す") {
				mockMvc.perform(
					patch("/authors/999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "夏目 金之助",
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
					)
						.andExpect(status().isNotFound)
						.andExpect(jsonPath("$.code").value("NOT_FOUND"))
						.andExpect(jsonPath("$.message").value("author not found: 999"))
						.andExpect(jsonPath("$.details.length()").value(0))

					dsl.fetchCount(AUTHORS) shouldBe 0
				}
		}

		context("著者名が空白の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "   ",
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
			}
		}

		context("著者名が未指定の場合") {
			it("既存の著者名は変更せずに更新する") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "birthDate": "1867-02-09"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.name").value("夏目漱石"))
					.andExpect(jsonPath("$.birthDate").value("1867-02-09"))

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
			}
		}

		context("生年月日が未指定の場合") {
			it("既存の生年月日は変更せずに更新する") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "夏目 金之助"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.name").value("夏目 金之助"))
					.andExpect(jsonPath("$.birthDate").value("1867-02-09"))

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目 金之助"
				author?.get(AUTHORS.BIRTH_DATE).toString() shouldBe "1867-02-09"
			}
		}

		context("著者名も生年月日も未指定の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"),
				)
					.andExpect(status().isBadRequest)

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
				author?.get(AUTHORS.BIRTH_DATE).toString() shouldBe "1867-02-09"
			}
		}

		context("生年月日が現在日より後の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					patch("/authors/$authorId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "name": "未来 太郎",
							  "birthDate": "2999-01-01"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				val author = dsl.selectFrom(AUTHORS).where(AUTHORS.ID.eq(authorId)).fetchOne()

				author?.get(AUTHORS.NAME) shouldBe "夏目漱石"
				author?.get(AUTHORS.BIRTH_DATE).toString() shouldBe "1867-02-09"
			}
		}
	}
}) {
	@Autowired
	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var dsl: DSLContext
}
