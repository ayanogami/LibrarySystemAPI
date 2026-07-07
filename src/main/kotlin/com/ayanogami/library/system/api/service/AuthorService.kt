package com.ayanogami.library.system.api.service

import com.ayanogami.library.system.api.exception.AuthorNotFoundException
import com.ayanogami.library.system.api.exception.InvalidAuthorException
import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.AuthorBooks
import com.ayanogami.library.system.api.repository.AuthorRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AuthorService(
	private val authorRepository: AuthorRepository,
) {
	fun create(name: String, birthDate: LocalDate): Author {
		validateName(name)
		validateBirthDate(birthDate)

		return authorRepository.create(name, birthDate)
	}

	fun update(id: Long, name: String?, birthDate: LocalDate?): Author {
		if (name == null && birthDate == null) {
			throw InvalidAuthorException("name or birthDate is required")
		}

		name?.let { validateName(it) }
		birthDate?.let { validateBirthDate(it) }

		val currentAuthor = authorRepository.findById(id)
			?: throw AuthorNotFoundException(id)
		val updatedName = name ?: currentAuthor.name
		val updatedBirthDate = birthDate ?: currentAuthor.birthDate

		return authorRepository.update(id, updatedName, updatedBirthDate)
			?: throw AuthorNotFoundException(id)
	}

	fun findBooks(id: Long): AuthorBooks {
		val author = authorRepository.findById(id)
			?: throw AuthorNotFoundException(id)

		return AuthorBooks(
			author = author,
			books = authorRepository.findBooksByAuthorId(id),
		)
	}

	private fun validateName(name: String) {
		if (name.isBlank()) {
			throw InvalidAuthorException("name is required")
		}
	}

	private fun validateBirthDate(birthDate: LocalDate) {
		if (birthDate.isAfter(LocalDate.now())) {
			throw InvalidAuthorException("birthDate must be today or earlier")
		}
	}
}
