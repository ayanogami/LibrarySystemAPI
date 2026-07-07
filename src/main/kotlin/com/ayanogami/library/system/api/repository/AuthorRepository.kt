package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.model.Book
import java.time.LocalDate

interface AuthorRepository {
	fun create(name: String, birthDate: LocalDate): Author

	fun findById(id: Long): Author?

	fun findBooksByAuthorId(authorId: Long): List<Book>

	fun update(id: Long, name: String, birthDate: LocalDate): Author?
}
