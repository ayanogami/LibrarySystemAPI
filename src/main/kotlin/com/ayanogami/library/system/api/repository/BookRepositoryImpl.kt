package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus
import com.ayanogami.library.system.api.jooq.generated.Tables.AUTHORS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOKS
import com.ayanogami.library.system.api.jooq.generated.Tables.BOOK_AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

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
					id = record.get(AUTHORS.ID)!!,
					name = record.get(AUTHORS.NAME)!!,
					birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
				)
			}
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

		val bookId = bookRecord.get(BOOKS.ID)!!

		authors.forEach { author ->
			dsl
				.insertInto(BOOK_AUTHORS)
				.set(BOOK_AUTHORS.BOOK_ID, bookId)
				.set(BOOK_AUTHORS.AUTHOR_ID, author.id)
				.execute()
		}

		return Book(
			id = bookId,
			title = bookRecord.get(BOOKS.TITLE)!!,
			price = bookRecord.get(BOOKS.PRICE)!!,
			publicationStatus = PublicationStatus.valueOf(bookRecord.get(BOOKS.PUBLICATION_STATUS)!!),
			authors = authors,
		)
	}
}
