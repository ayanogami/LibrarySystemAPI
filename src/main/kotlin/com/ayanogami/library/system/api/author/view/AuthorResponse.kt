package com.ayanogami.library.system.api.author.view

import com.ayanogami.library.system.api.author.model.Author
import java.time.LocalDate

data class AuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
) {
	companion object {
		fun from(author: Author): AuthorResponse =
			AuthorResponse(
				id = author.id,
				name = author.name,
				birthDate = author.birthDate,
			)
	}
}
