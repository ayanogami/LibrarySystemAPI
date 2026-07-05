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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class BookApiSpec : DescribeSpec({
	extension(SpringExtension())

	lateinit var mockMvc: MockMvc
	lateinit var dsl: DSLContext

	beforeSpec { spec ->
		val bookApiSpec = spec as BookApiSpec
		mockMvc = bookApiSpec.mockMvc
		dsl = bookApiSpec.dsl
	}

	beforeTest {
		dsl.deleteFrom(BOOK_AUTHORS).execute()
		dsl.deleteFrom(BOOKS).execute()
		dsl.deleteFrom(AUTHORS).execute()
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

	describe("POST /books") {
		context("リクエストが妥当な場合") {
			it("書籍を作成する") {
				val authorId = createAuthor()

				mockMvc.perform(
					post("/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [$authorId],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isCreated)
					.andExpect(jsonPath("$.id").isNumber)
					.andExpect(jsonPath("$.title").value("吾輩は猫である"))
					.andExpect(jsonPath("$.price").value(1200))
					.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
					.andExpect(jsonPath("$.authors[0].id").value(authorId))
					.andExpect(jsonPath("$.authors[0].name").value("夏目漱石"))
					.andExpect(jsonPath("$.authors[0].birthDate").value("1867-02-09"))

				val book = dsl.selectFrom(BOOKS).fetchOne()
				val bookAuthor = dsl.selectFrom(BOOK_AUTHORS).fetchOne()

				book?.get(BOOKS.TITLE) shouldBe "吾輩は猫である"
				book?.get(BOOKS.PRICE) shouldBe 1200
				book?.get(BOOKS.PUBLICATION_STATUS) shouldBe "PUBLISHED"
				bookAuthor?.get(BOOK_AUTHORS.BOOK_ID) shouldBe book?.get(BOOKS.ID)
				bookAuthor?.get(BOOK_AUTHORS.AUTHOR_ID) shouldBe authorId
			}
		}

		context("複数の著者IDが指定された場合") {
			it("書籍と著者の関連を作成する") {
				val firstAuthorId = createAuthor("夏目漱石")
				val secondAuthorId = createAuthor("森鴎外", LocalDate.of(1862, 2, 17))

				mockMvc.perform(
					post("/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "近代文学選集",
							  "price": 2000,
							  "authorIds": [$firstAuthorId, $secondAuthorId],
							  "publicationStatus": "UNPUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isCreated)
					.andExpect(jsonPath("$.authors.length()").value(2))
					.andExpect(jsonPath("$.authors[0].id").value(firstAuthorId))
					.andExpect(jsonPath("$.authors[1].id").value(secondAuthorId))

				dsl.fetchCount(BOOKS) shouldBe 1
				dsl.fetchCount(BOOK_AUTHORS) shouldBe 2
			}
		}

		context("価格が負数の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					post("/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": -1,
							  "authorIds": [$authorId],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(BOOKS) shouldBe 0
				dsl.fetchCount(BOOK_AUTHORS) shouldBe 0
			}
		}

		context("著者IDが空の場合") {
			it("400 Bad Request を返す") {
				mockMvc.perform(
					post("/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(BOOKS) shouldBe 0
				dsl.fetchCount(BOOK_AUTHORS) shouldBe 0
			}
		}

		context("存在しない著者IDが含まれる場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					post("/books")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [$authorId, 999999],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(BOOKS) shouldBe 0
				dsl.fetchCount(BOOK_AUTHORS) shouldBe 0
			}
		}
	}

	describe("PUT /books/{bookId}") {
		context("リクエストが妥当な場合") {
			it("書籍情報と著者関連を更新する") {
				val currentAuthorId = createAuthor("夏目漱石")
				val firstUpdatedAuthorId = createAuthor("森鴎外", LocalDate.of(1862, 2, 17))
				val secondUpdatedAuthorId = createAuthor("芥川龍之介", LocalDate.of(1892, 3, 1))
				val bookId = createBook(authorIds = listOf(currentAuthorId))

				mockMvc.perform(
					put("/books/$bookId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "近代文学選集",
							  "price": 2000,
							  "authorIds": [$firstUpdatedAuthorId, $secondUpdatedAuthorId],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isOk)
					.andExpect(jsonPath("$.id").value(bookId))
					.andExpect(jsonPath("$.title").value("近代文学選集"))
					.andExpect(jsonPath("$.price").value(2000))
					.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
					.andExpect(jsonPath("$.authors.length()").value(2))
					.andExpect(jsonPath("$.authors[0].id").value(firstUpdatedAuthorId))
					.andExpect(jsonPath("$.authors[1].id").value(secondUpdatedAuthorId))

				val book = dsl.selectFrom(BOOKS).where(BOOKS.ID.eq(bookId)).fetchOne()
				val bookAuthors = dsl
					.selectFrom(BOOK_AUTHORS)
					.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
					.orderBy(BOOK_AUTHORS.AUTHOR_ID)
					.fetch()

				book?.get(BOOKS.TITLE) shouldBe "近代文学選集"
				book?.get(BOOKS.PRICE) shouldBe 2000
				book?.get(BOOKS.PUBLICATION_STATUS) shouldBe "PUBLISHED"
				bookAuthors.map { it.get(BOOK_AUTHORS.AUTHOR_ID) } shouldBe listOf(
					firstUpdatedAuthorId,
					secondUpdatedAuthorId,
				)
			}
		}

		context("書籍IDが存在しない場合") {
			it("404 Not Found を返す") {
				val authorId = createAuthor()

				mockMvc.perform(
					put("/books/999999")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [$authorId],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isNotFound)

				dsl.fetchCount(BOOKS) shouldBe 0
				dsl.fetchCount(BOOK_AUTHORS) shouldBe 0
			}
		}

		context("価格が負数の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()
				val bookId = createBook(authorIds = listOf(authorId))

				mockMvc.perform(
					put("/books/$bookId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": -1,
							  "authorIds": [$authorId],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				val book = dsl.selectFrom(BOOKS).where(BOOKS.ID.eq(bookId)).fetchOne()

				book?.get(BOOKS.PRICE) shouldBe 1200
			}
		}

		context("著者IDが空の場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()
				val bookId = createBook(authorIds = listOf(authorId))

				mockMvc.perform(
					put("/books/$bookId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(BOOK_AUTHORS) shouldBe 1
			}
		}

		context("存在しない著者IDが含まれる場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()
				val bookId = createBook(authorIds = listOf(authorId))

				mockMvc.perform(
					put("/books/$bookId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [$authorId, 999999],
							  "publicationStatus": "PUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				dsl.fetchCount(BOOK_AUTHORS) shouldBe 1
			}
		}

		context("出版済みの書籍を未出版に戻す場合") {
			it("400 Bad Request を返す") {
				val authorId = createAuthor()
				val bookId = createBook(
					publicationStatus = "PUBLISHED",
					authorIds = listOf(authorId),
				)

				mockMvc.perform(
					put("/books/$bookId")
						.contentType(MediaType.APPLICATION_JSON)
						.content(
							"""
							{
							  "title": "吾輩は猫である",
							  "price": 1200,
							  "authorIds": [$authorId],
							  "publicationStatus": "UNPUBLISHED"
							}
							""".trimIndent(),
						),
				)
					.andExpect(status().isBadRequest)

				val book = dsl.selectFrom(BOOKS).where(BOOKS.ID.eq(bookId)).fetchOne()

				book?.get(BOOKS.PUBLICATION_STATUS) shouldBe "PUBLISHED"
			}
		}
	}
}) {
	@Autowired
	lateinit var mockMvc: MockMvc

	@Autowired
	lateinit var dsl: DSLContext
}
