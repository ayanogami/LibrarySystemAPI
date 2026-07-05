package com.ayanogami.library.system.api

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.exception.AuthorIdsRequiredException
import com.ayanogami.library.system.api.exception.BookAuthorNotFoundException
import com.ayanogami.library.system.api.exception.InvalidBookException
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus
import com.ayanogami.library.system.api.repository.BookRepository
import com.ayanogami.library.system.api.service.BookService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class BookServiceSpec : DescribeSpec({
	describe("書籍作成") {
		context("リクエストが妥当な場合") {
			it("作成された書籍を返す") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				val author = Author(
					id = 1,
					name = "夏目漱石",
					birthDate = LocalDate.of(1867, 2, 9),
				)
				val book = Book(
					id = 1,
					title = "吾輩は猫である",
					price = 1200,
					publicationStatus = PublicationStatus.PUBLISHED,
					authors = listOf(author),
				)
				every { repository.findAuthorsByIds(listOf(1)) } returns listOf(author)
				every {
					repository.create(
						"吾輩は猫である",
						1200,
						PublicationStatus.PUBLISHED,
						listOf(author),
					)
				} returns book

				val createdBook = service.create(
					title = "吾輩は猫である",
					price = 1200,
					authorIds = listOf(1),
					publicationStatus = PublicationStatus.PUBLISHED,
				)

				createdBook shouldBe book
				verify(exactly = 1) { repository.findAuthorsByIds(listOf(1)) }
				verify(exactly = 1) {
					repository.create(
						"吾輩は猫である",
						1200,
						PublicationStatus.PUBLISHED,
						listOf(author),
					)
				}
			}
		}

		context("書籍タイトルが空白の場合") {
			it("InvalidBookException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)

				val exception = shouldThrow<InvalidBookException> {
					service.create("   ", 1200, listOf(1), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "title is required"
				verify(exactly = 0) { repository.findAuthorsByIds(any()) }
				verify(exactly = 0) { repository.create(any(), any(), any(), any()) }
			}
		}

		context("価格が負数の場合") {
			it("InvalidBookException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)

				val exception = shouldThrow<InvalidBookException> {
					service.create("吾輩は猫である", -1, listOf(1), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "price must be 0 or greater"
				verify(exactly = 0) { repository.findAuthorsByIds(any()) }
				verify(exactly = 0) { repository.create(any(), any(), any(), any()) }
			}
		}

		context("著者IDが空の場合") {
			it("AuthorIdsRequiredException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)

				val exception = shouldThrow<AuthorIdsRequiredException> {
					service.create("吾輩は猫である", 1200, emptyList(), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "authorIds is required"
				verify(exactly = 0) { repository.findAuthorsByIds(any()) }
				verify(exactly = 0) { repository.create(any(), any(), any(), any()) }
			}
		}

		context("存在しない著者IDが含まれる場合") {
			it("BookAuthorNotFoundException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				every { repository.findAuthorsByIds(listOf(1, 999)) } returns listOf(
					Author(
						id = 1,
						name = "夏目漱石",
						birthDate = LocalDate.of(1867, 2, 9),
					),
				)

				val exception = shouldThrow<BookAuthorNotFoundException> {
					service.create("吾輩は猫である", 1200, listOf(1, 999), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "author not found: 999"
				verify(exactly = 1) { repository.findAuthorsByIds(listOf(1, 999)) }
				verify(exactly = 0) { repository.create(any(), any(), any(), any()) }
			}
		}
	}
})
