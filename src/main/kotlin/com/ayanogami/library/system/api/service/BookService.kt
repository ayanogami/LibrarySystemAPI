package com.ayanogami.library.system.api.service

import com.ayanogami.library.system.api.exception.AuthorIdsRequiredException
import com.ayanogami.library.system.api.exception.BookAuthorNotFoundException
import com.ayanogami.library.system.api.exception.BookNotFoundException
import com.ayanogami.library.system.api.exception.InvalidBookException
import com.ayanogami.library.system.api.model.Book
import com.ayanogami.library.system.api.model.PublicationStatus
import com.ayanogami.library.system.api.repository.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
	private val bookRepository: BookRepository,
) {
	@Transactional
	fun create(
		title: String,
		price: Int,
		authorIds: List<Long>,
		publicationStatus: PublicationStatus,
	): Book {
		validateTitle(title)
		validatePrice(price)
		validateAuthorIds(authorIds)
		val authors = findAuthors(authorIds)

		return bookRepository.create(title, price, publicationStatus, authors)
	}

	@Transactional
	fun update(
		id: Long,
		title: String?,
		price: Int?,
		authorIds: List<Long>?,
		publicationStatus: PublicationStatus?,
	): Book {
		validateUpdateRequest(title, price, authorIds, publicationStatus)
		title?.let { validateTitle(it) }
		price?.let { validatePrice(it) }
		authorIds?.let { validateAuthorIds(it) }

		val currentBook = bookRepository.findById(id)
			?: throw BookNotFoundException(id)
		val updatedPublicationStatus = publicationStatus ?: currentBook.publicationStatus

		if (currentBook.publicationStatus == PublicationStatus.PUBLISHED &&
			updatedPublicationStatus == PublicationStatus.UNPUBLISHED
		) {
			throw InvalidBookException("published book cannot be unpublished")
		}

		val updatedTitle = title ?: currentBook.title
		val updatedPrice = price ?: currentBook.price
		val authors = authorIds?.let { findAuthors(it) } ?: currentBook.authors

		return bookRepository.update(id, updatedTitle, updatedPrice, updatedPublicationStatus, authors)
			?: throw BookNotFoundException(id)
	}

	private fun validateTitle(title: String) {
		if (title.isBlank()) {
			throw InvalidBookException("title is required")
		}
	}

	private fun validatePrice(price: Int) {
		if (price < 0) {
			throw InvalidBookException("price must be 0 or greater")
		}
	}

	private fun validateAuthorIds(authorIds: List<Long>) {
		if (authorIds.isEmpty()) {
			throw AuthorIdsRequiredException()
		}
	}

	private fun validateUpdateRequest(
		title: String?,
		price: Int?,
		authorIds: List<Long>?,
		publicationStatus: PublicationStatus?,
	) {
		if (title == null && price == null && authorIds == null && publicationStatus == null) {
			throw InvalidBookException("title, price, authorIds, or publicationStatus is required")
		}
	}

	private fun findAuthors(authorIds: List<Long>) =
		authorIds.distinct().let { distinctAuthorIds ->
			val authors = bookRepository.findAuthorsByIds(distinctAuthorIds)
			val foundAuthorIds = authors.map { it.id }.toSet()
			val missingAuthorIds = distinctAuthorIds.filterNot(foundAuthorIds::contains)

			if (missingAuthorIds.isNotEmpty()) {
				throw BookAuthorNotFoundException(missingAuthorIds)
			}

			authors
		}
}
