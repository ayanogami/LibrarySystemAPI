package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.model.Author
import com.ayanogami.library.system.api.jooq.generated.Tables.AUTHORS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

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
			id = requireRecordValue(record.get(AUTHORS.ID), "author id"),
			name = requireRecordValue(record.get(AUTHORS.NAME), "author name"),
			birthDate = requireRecordValue(record.get(AUTHORS.BIRTH_DATE), "author birth date"),
		)
	}

	override fun findById(id: Long): Author? {
		val record = dsl
			.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.from(AUTHORS)
			.where(AUTHORS.ID.eq(id))
			.fetchOne()
			?: return null

		return Author(
			id = requireRecordValue(record.get(AUTHORS.ID), "author id"),
			name = requireRecordValue(record.get(AUTHORS.NAME), "author name"),
			birthDate = requireRecordValue(record.get(AUTHORS.BIRTH_DATE), "author birth date"),
		)
	}

	override fun update(id: Long, name: String, birthDate: LocalDate): Author? {
		val record = dsl
			.update(AUTHORS)
			.set(AUTHORS.NAME, name)
			.set(AUTHORS.BIRTH_DATE, birthDate)
			.set(AUTHORS.UPDATED_AT, LocalDateTime.now())
			.where(AUTHORS.ID.eq(id))
			.returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
			.fetchOne()
			?: return null

		return Author(
			id = requireRecordValue(record.get(AUTHORS.ID), "author id"),
			name = requireRecordValue(record.get(AUTHORS.NAME), "author name"),
			birthDate = requireRecordValue(record.get(AUTHORS.BIRTH_DATE), "author birth date"),
		)
	}
}
