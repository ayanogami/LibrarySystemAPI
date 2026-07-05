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
