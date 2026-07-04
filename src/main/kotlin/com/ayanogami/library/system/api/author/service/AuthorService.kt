package com.ayanogami.library.system.api.author.service

import com.ayanogami.library.system.api.author.exception.InvalidAuthorException
import com.ayanogami.library.system.api.author.model.Author
import com.ayanogami.library.system.api.author.repository.AuthorRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class AuthorService(
	private val authorRepository: AuthorRepository,
) {
	fun create(name: String, birthDate: LocalDate): Author {
		if (birthDate.isAfter(LocalDate.now())) {
			throw InvalidAuthorException("birthDate must be today or earlier")
		}

		return authorRepository.create(name, birthDate)
	}
}
