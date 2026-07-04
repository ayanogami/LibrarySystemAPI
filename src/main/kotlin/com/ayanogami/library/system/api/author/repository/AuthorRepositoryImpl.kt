package com.ayanogami.library.system.api.author.repository

import com.ayanogami.library.system.api.author.model.Author
import com.ayanogami.library.system.api.jooq.generated.Tables.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AuthorRepositoryImpl(
	private val dsl: DSLContext,
) : AuthorRepository {
	override fun create(name: String, birthDate: LocalDate): Author {
		val record = dsl
			.insertInto(AUTHORS)
			.set(AUTHORS.NAME, name)
			.set(AUTHORS.BIRTH_DATE, birthDate)
			.returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.fetchOne()
			?: error("Failed to create author")

		return Author(
			id = record.get(AUTHORS.ID)!!,
			name = record.get(AUTHORS.NAME)!!,
			birthDate = record.get(AUTHORS.BIRTH_DATE)!!,
		)
	}
}
