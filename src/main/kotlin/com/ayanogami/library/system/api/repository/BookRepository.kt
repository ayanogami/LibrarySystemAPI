package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus

interface BookRepository {
	fun findAuthorsByIds(authorIds: Collection<Long>): List<Author>

	fun findById(id: Long): Book?

	fun create(
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
		authors: List<Author>,
	): Book

	fun update(
		id: Long,
		title: String,
		price: Int,
		publicationStatus: PublicationStatus,
		authors: List<Author>,
	): Book?
}
