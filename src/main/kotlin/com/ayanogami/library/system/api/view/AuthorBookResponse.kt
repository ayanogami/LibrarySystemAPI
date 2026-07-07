package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus

data class AuthorBookResponse(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
) {
	companion object {
		fun from(book: Book): AuthorBookResponse =
			AuthorBookResponse(
				id = book.id,
				title = book.title,
				price = book.price,
				publicationStatus = book.publicationStatus,
			)
	}
}
