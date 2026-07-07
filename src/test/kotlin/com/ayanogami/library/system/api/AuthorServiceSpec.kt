package com.ayanogami.library.system.api

import com.ayanogami.library.system.api.exception.AuthorNotFoundException
import com.ayanogami.library.system.api.exception.InvalidAuthorException
import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.AuthorBooks
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus
import com.ayanogami.library.system.api.repository.AuthorRepository
import com.ayanogami.library.system.api.service.AuthorService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class AuthorServiceSpec : DescribeSpec({
	describe("著者作成") {
		context("リクエストが妥当な場合") {
			it("作成された著者を返す") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val birthDate = LocalDate.of(1867, 2, 9)
				every { repository.create("夏目漱石", birthDate) } returns Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)

				val author = service.create("夏目漱石", birthDate)

				author shouldBe Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)
				verify(exactly = 1) { repository.create("夏目漱石", birthDate) }
			}
		}

		context("生年月日が現在日の場合") {
			it("作成された著者を返す") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val today = LocalDate.now()
				every { repository.create("今日 生まれ", today) } returns Author(
					id = 1,
					name = "今日 生まれ",
					birthDate = today,
				)

				val author = service.create("今日 生まれ", today)

				author.birthDate shouldBe today
				verify(exactly = 1) { repository.create("今日 生まれ", today) }
			}
		}

		context("著者名が空白の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)

				val exception = shouldThrow<InvalidAuthorException> {
					service.create("   ", LocalDate.of(1867, 2, 9))
				}

				exception.message shouldBe "name is required"
				verify(exactly = 0) { repository.create(any(), any()) }
			}
		}

		context("生年月日が現在日より後の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val tomorrow = LocalDate.now().plusDays(1)

				val exception = shouldThrow<InvalidAuthorException> {
					service.create("未来 太郎", tomorrow)
				}

				exception.message shouldBe "birthDate must be today or earlier"
				verify(exactly = 0) { repository.create(any(), any()) }
			}
			}
		}

		describe("著者別書籍一覧取得") {
			context("著者に書籍が紐づく場合") {
				it("著者情報と書籍一覧を返す") {
					val repository = mockk<AuthorRepository>()
					val service = AuthorService(repository)
					val author = Author(
						id = 1,
						name = "夏目漱石",
						birthDate = LocalDate.of(1867, 2, 9),
					)
					val books = listOf(
						Book(
							id = 1,
							title = "吾輩は猫である",
							price = 1200,
							publicationStatus = PublicationStatus.PUBLISHED,
							authors = listOf(author),
						),
						Book(
							id = 2,
							title = "坊っちゃん",
							price = 900,
							publicationStatus = PublicationStatus.UNPUBLISHED,
							authors = listOf(author),
						),
					)
					every { repository.findById(1) } returns author
					every { repository.findBooksByAuthorId(1) } returns books

					val authorBooks = service.findBooks(1)

					authorBooks shouldBe AuthorBooks(author, books)
					verify(exactly = 1) { repository.findById(1) }
					verify(exactly = 1) { repository.findBooksByAuthorId(1) }
				}
			}

			context("著者に紐づく書籍がない場合") {
				it("空の書籍一覧を返す") {
					val repository = mockk<AuthorRepository>()
					val service = AuthorService(repository)
					val author = Author(
						id = 1,
						name = "夏目漱石",
						birthDate = LocalDate.of(1867, 2, 9),
					)
					every { repository.findById(1) } returns author
					every { repository.findBooksByAuthorId(1) } returns emptyList()

					val authorBooks = service.findBooks(1)

					authorBooks shouldBe AuthorBooks(author, emptyList())
					verify(exactly = 1) { repository.findById(1) }
					verify(exactly = 1) { repository.findBooksByAuthorId(1) }
				}
			}

			context("著者IDが存在しない場合") {
				it("AuthorNotFoundException を投げる") {
					val repository = mockk<AuthorRepository>()
					val service = AuthorService(repository)
					every { repository.findById(999) } returns null

					val exception = shouldThrow<AuthorNotFoundException> {
						service.findBooks(999)
					}

					exception.message shouldBe "author not found: 999"
					verify(exactly = 1) { repository.findById(999) }
					verify(exactly = 0) { repository.findBooksByAuthorId(any()) }
				}
			}
		}

		describe("著者更新") {
			context("著者名のみ指定された場合") {
				it("著者名だけを更新する") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val birthDate = LocalDate.of(1867, 2, 9)
				every { repository.findById(1) } returns Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)
				every { repository.update(1, "夏目 金之助", birthDate) } returns Author(
					id = 1,
					name = "夏目 金之助",
					birthDate = birthDate,
				)

				val author = service.update(1, "夏目 金之助", null)

				author shouldBe Author(
					id = 1,
					name = "夏目 金之助",
					birthDate = birthDate,
				)
				verify(exactly = 1) { repository.findById(1) }
				verify(exactly = 1) { repository.update(1, "夏目 金之助", birthDate) }
			}
		}

		context("生年月日のみ指定された場合") {
			it("生年月日だけを更新する") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val birthDate = LocalDate.of(1867, 2, 9)
				val updatedBirthDate = LocalDate.of(1867, 2, 10)
				every { repository.findById(1) } returns Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)
				every { repository.update(1, "夏目漱石", updatedBirthDate) } returns Author(
					id = 1,
					name = "夏目漱石",
					birthDate = updatedBirthDate,
				)

				val author = service.update(1, null, updatedBirthDate)

				author shouldBe Author(
					id = 1,
					name = "夏目漱石",
					birthDate = updatedBirthDate,
				)
				verify(exactly = 1) { repository.findById(1) }
				verify(exactly = 1) { repository.update(1, "夏目漱石", updatedBirthDate) }
			}
		}

		context("著者IDが存在しない場合") {
			it("AuthorNotFoundException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				every { repository.findById(999) } returns null

				val exception = shouldThrow<AuthorNotFoundException> {
					service.update(999, "夏目 金之助", null)
				}

				exception.message shouldBe "author not found: 999"
				verify(exactly = 1) { repository.findById(999) }
				verify(exactly = 0) { repository.update(any(), any(), any()) }
			}
		}

		context("著者名も生年月日も未指定の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)

				val exception = shouldThrow<InvalidAuthorException> {
					service.update(1, null, null)
				}

				exception.message shouldBe "name or birthDate is required"
				verify(exactly = 0) { repository.findById(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any()) }
			}
		}

		context("著者名が空白の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)

				val exception = shouldThrow<InvalidAuthorException> {
					service.update(1, "   ", LocalDate.of(1867, 2, 9))
				}

				exception.message shouldBe "name is required"
				verify(exactly = 0) { repository.findById(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any()) }
			}
		}

		context("生年月日が現在日より後の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val tomorrow = LocalDate.now().plusDays(1)

				val exception = shouldThrow<InvalidAuthorException> {
					service.update(1, "未来 太郎", tomorrow)
				}

				exception.message shouldBe "birthDate must be today or earlier"
				verify(exactly = 0) { repository.findById(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any()) }
			}
		}
	}
})
