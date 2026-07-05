package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus

interface BookRepository {
	fun findAuthorsByIds(authorIds: Collection<Long>): List<Author>

	fun create(
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
		authors: List<Author>,
	): Book
}
