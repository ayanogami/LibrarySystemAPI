package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.Author
import java.time.LocalDate

data class BookAuthorResponse(
	val id: Long,
	val name: String,
	val birthDate: LocalDate,
) {
	companion object {
		fun from(author: Author): BookAuthorResponse =
			BookAuthorResponse(
				id = author.id,
				name = author.name,
				birthDate = author.birthDate,
			)
	}
}
