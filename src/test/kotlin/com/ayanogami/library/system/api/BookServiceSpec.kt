package com.ayanogami.library.system.api

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.exception.AuthorIdsRequiredException
import com.ayanogami.library.system.api.exception.BookAuthorNotFoundException
import com.ayanogami.library.system.api.exception.BookNotFoundException
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

	describe("書籍更新") {
		context("リクエストが妥当な場合") {
			it("更新された書籍を返す") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				val currentAuthor = Author(
					id = 1,
					name = "夏目漱石",
					birthDate = LocalDate.of(1867, 2, 9),
				)
				val updatedAuthor = Author(
					id = 2,
					name = "森鴎外",
					birthDate = LocalDate.of(1862, 2, 17),
				)
				val currentBook = Book(
					id = 1,
					title = "吾輩は猫である",
					price = 1200,
					publicationStatus = PublicationStatus.UNPUBLISHED,
					authors = listOf(currentAuthor),
				)
				val updatedBook = Book(
					id = 1,
					title = "舞姫",
					price = 900,
					publicationStatus = PublicationStatus.PUBLISHED,
					authors = listOf(updatedAuthor),
				)
				every { repository.findById(1) } returns currentBook
				every { repository.findAuthorsByIds(listOf(2)) } returns listOf(updatedAuthor)
				every {
					repository.update(
						1,
						"舞姫",
						900,
						PublicationStatus.PUBLISHED,
						listOf(updatedAuthor),
					)
				} returns updatedBook

				val book = service.update(
					id = 1,
					title = "舞姫",
					price = 900,
					authorIds = listOf(2),
					publicationStatus = PublicationStatus.PUBLISHED,
				)

				book shouldBe updatedBook
				verify(exactly = 1) { repository.findById(1) }
				verify(exactly = 1) { repository.findAuthorsByIds(listOf(2)) }
				verify(exactly = 1) {
					repository.update(
						1,
						"舞姫",
						900,
						PublicationStatus.PUBLISHED,
						listOf(updatedAuthor),
					)
				}
			}
		}

		context("書籍IDが存在しない場合") {
			it("BookNotFoundException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				every { repository.findById(999) } returns null

				val exception = shouldThrow<BookNotFoundException> {
					service.update(999, "吾輩は猫である", 1200, listOf(1), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "book not found: 999"
				verify(exactly = 1) { repository.findById(999) }
				verify(exactly = 0) { repository.findAuthorsByIds(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any(), any(), any()) }
			}
		}

		context("価格が負数の場合") {
			it("InvalidBookException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)

				val exception = shouldThrow<InvalidBookException> {
					service.update(1, "吾輩は猫である", -1, listOf(1), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "price must be 0 or greater"
				verify(exactly = 0) { repository.findById(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any(), any(), any()) }
			}
		}

		context("著者IDが空の場合") {
			it("AuthorIdsRequiredException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)

				val exception = shouldThrow<AuthorIdsRequiredException> {
					service.update(1, "吾輩は猫である", 1200, emptyList(), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "authorIds is required"
				verify(exactly = 0) { repository.findById(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any(), any(), any()) }
			}
		}

		context("出版済みの書籍を未出版に戻す場合") {
			it("InvalidBookException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				val author = Author(
					id = 1,
					name = "夏目漱石",
					birthDate = LocalDate.of(1867, 2, 9),
				)
				every { repository.findById(1) } returns Book(
					id = 1,
					title = "吾輩は猫である",
					price = 1200,
					publicationStatus = PublicationStatus.PUBLISHED,
					authors = listOf(author),
				)

				val exception = shouldThrow<InvalidBookException> {
					service.update(1, "吾輩は猫である", 1200, listOf(1), PublicationStatus.UNPUBLISHED)
				}

				exception.message shouldBe "published book cannot be unpublished"
				verify(exactly = 1) { repository.findById(1) }
				verify(exactly = 0) { repository.findAuthorsByIds(any()) }
				verify(exactly = 0) { repository.update(any(), any(), any(), any(), any()) }
			}
		}

		context("存在しない著者IDが含まれる場合") {
			it("BookAuthorNotFoundException を投げる") {
				val repository = mockk<BookRepository>()
				val service = BookService(repository)
				val author = Author(
					id = 1,
					name = "夏目漱石",
					birthDate = LocalDate.of(1867, 2, 9),
				)
				every { repository.findById(1) } returns Book(
					id = 1,
					title = "吾輩は猫である",
					price = 1200,
					publicationStatus = PublicationStatus.UNPUBLISHED,
					authors = listOf(author),
				)
				every { repository.findAuthorsByIds(listOf(1, 999)) } returns listOf(author)

				val exception = shouldThrow<BookAuthorNotFoundException> {
					service.update(1, "吾輩は猫である", 1200, listOf(1, 999), PublicationStatus.PUBLISHED)
				}

				exception.message shouldBe "author not found: 999"
				verify(exactly = 1) { repository.findById(1) }
				verify(exactly = 1) { repository.findAuthorsByIds(listOf(1, 999)) }
				verify(exactly = 0) { repository.update(any(), any(), any(), any(), any()) }
			}
		}
	}
})
