package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus
import com.ayanogami.library.system.api.jooq.generated.Tables.AUTHORS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOKS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOK_AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class BookRepositoryImpl(
	private val dsl: DSLContext,
) : BookRepository {
	override fun findAuthorsByIds(authorIds: Collection<Long>): List<Author> {
		if (authorIds.isEmpty()) {
			return emptyList()
		}

		return dsl
			.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.from(AUTHORS)
			.where(AUTHORS.ID.`in`(authorIds))
			.orderBy(AUTHORS.ID)
			.fetch { record ->
				Author(
					id = requireRecordValue(record.get(AUTHORS.ID), "author id"),
					name = requireRecordValue(record.get(AUTHORS.NAME), "author name"),
					birthDate = requireRecordValue(record.get(AUTHORS.BIRTH_DATE), "author birth date"),
				)
			}
	}

	override fun findById(id: Long): Book? {
		val bookRecord = dsl
			.select(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
			.from(BOOKS)
			.where(BOOKS.ID.eq(id))
			.fetchOne()
			?: return null

		return Book(
			id = requireRecordValue(bookRecord.get(BOOKS.ID), "book id"),
			title = requireRecordValue(bookRecord.get(BOOKS.TITLE), "book title"),
			price = requireRecordValue(bookRecord.get(BOOKS.PRICE), "book price"),
			publicationStatus = PublicationStatus.valueOf(
				requireRecordValue(bookRecord.get(BOOKS.PUBLICATION_STATUS), "book publication status"),
			),
			authors = findAuthorsByBookId(id),
		)
	}

	override fun create(
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
		authors: List<Author>,
	): Book {
		val bookRecord = dsl
			.insertInto(BOOKS)
			.set(BOOKS.TITLE, title)
			.set(BOOKS.PRICE, price)
			.set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
			.returning(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
			.fetchOne()
			?: error("Failed to create book")

		val bookId = requireRecordValue(bookRecord.get(BOOKS.ID), "book id")

		authors.forEach { author ->
			dsl
				.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, bookId)
				.set(BOOK_AUTHORS.AUTHOR_ID, author.id)
				.execute()
		}

		return Book(
			id = bookId,
			title = requireRecordValue(bookRecord.get(BOOKS.TITLE), "book title"),
			price = requireRecordValue(bookRecord.get(BOOKS.PRICE), "book price"),
			publicationStatus = PublicationStatus.valueOf(
				requireRecordValue(bookRecord.get(BOOKS.PUBLICATION_STATUS), "book publication status"),
			),
			authors = authors,
		)
	}

	override fun update(
		id: Long,
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
		authors: List<Author>,
	): Book? {
		val bookRecord = dsl
			.update(BOOKS)
			.set(BOOKS.TITLE, title)
			.set(BOOKS.PRICE, price)
			.set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
			.set(BOOKS.UPDATED_AT, LocalDateTime.now())
			.where(BOOKS.ID.eq(id))
			.returning(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
			.fetchOne()
			?: return null

		dsl.deleteFrom(BOOK_AUTHORS)
			.where(BOOK_AUTHORS.BOOK_ID.eq(id))
			.execute()

		authors.forEach { author ->
			dsl
				.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, id)
				.set(BOOK_AUTHORS.AUTHOR_ID, author.id)
				.execute()
		}

		return Book(
			id = requireRecordValue(bookRecord.get(BOOKS.ID), "book id"),
			title = requireRecordValue(bookRecord.get(BOOKS.TITLE), "book title"),
			price = requireRecordValue(bookRecord.get(BOOKS.PRICE), "book price"),
			publicationStatus = PublicationStatus.valueOf(
				requireRecordValue(bookRecord.get(BOOKS.PUBLICATION_STATUS), "book publication status"),
			),
			authors = authors,
		)
	}

	private fun findAuthorsByBookId(bookId: Long): List<Author> =
		dsl
			.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.from(AUTHORS)
			.join(BOOK_AUTHORS)
			.on(BOOK_AUTHORS.AUTHOR_ID.eq(AUTHORS.ID))
			.where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
			.orderBy(AUTHORS.ID)
			.fetch { record ->
				Author(
					id = requireRecordValue(record.get(AUTHORS.ID), "author id"),
					name = requireRecordValue(record.get(AUTHORS.NAME), "author name"),
					birthDate = requireRecordValue(record.get(AUTHORS.BIRTH_DATE), "author birth date"),
				)
			}
}
