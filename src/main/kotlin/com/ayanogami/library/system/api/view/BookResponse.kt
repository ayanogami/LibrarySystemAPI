package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus

data class BookResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
	val authors: List<BookAuthorResponse>,
) {
	companion object {
		fun from(book: Book): BookResponse =
			BookResponse(
				id = book.id,
				title = book.title,
				price = book.price,
				publicationStatus = book.publicationStatus,
				authors = book.authors.map(BookAuthorResponse::from),
			)
	}
}
