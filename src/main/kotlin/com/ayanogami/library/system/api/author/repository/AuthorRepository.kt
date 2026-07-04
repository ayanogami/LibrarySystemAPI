package com.ayanogami.library.system.api.author.repository

import com.ayanogami.library.system.api.author.model.Author
import java.time.LocalDate

interface AuthorRepository {
	fun create(name: String, birthDate: LocalDate): Author
}
