package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.AuthorBooks
import java.time.LocalDate

data class AuthorBooksResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
	val books: List<AuthorBookResponse>,
) {
	companion object {
		fun from(authorBooks: AuthorBooks): AuthorBooksResponse =
			AuthorBooksResponse(
				id = authorBooks.author.id,
				name = authorBooks.author.name,
				birthDate = authorBooks.author.birthDate,
				books = authorBooks.books.map(AuthorBookResponse::from),
			)
	}
}
