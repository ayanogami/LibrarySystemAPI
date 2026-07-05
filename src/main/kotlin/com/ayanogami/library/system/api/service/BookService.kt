package com.ayanogami.library.system.api.service

import com.ayanogami.library.system.api.exception.AuthorIdsRequiredException
import com.ayanogami.library.system.api.exception.BookAuthorNotFoundException
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

		val distinctAuthorIds = authorIds.distinct()
		val authors = bookRepository.findAuthorsByIds(distinctAuthorIds)
		val foundAuthorIds = authors.map { it.id }.toSet()
		val missingAuthorIds = distinctAuthorIds.filterNot(foundAuthorIds::contains)

		if (missingAuthorIds.isNotEmpty()) {
			throw BookAuthorNotFoundException(missingAuthorIds)
		}

		return bookRepository.create(title, price, publicationStatus, authors)
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
}
