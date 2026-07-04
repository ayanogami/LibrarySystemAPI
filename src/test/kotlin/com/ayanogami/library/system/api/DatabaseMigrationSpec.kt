package com.ayanogami.library.system.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class DatabaseMigrationSpec : FunSpec({
	extension(SpringExtension())

	lateinit var dsl: DSLContext

	beforeSpec { spec ->
		dsl = (spec as DatabaseMigrationSpec).dsl
	}

	test("Flyway creates library tables") {
		val tableNames = dsl
			.fetch(
				"""
				select table_name
				from information_schema.tables
				where table_schema = 'public'
				  and table_type = 'BASE TABLE'
				""".trimIndent(),
			)
			.map { it.get("table_name", String::class.java) }

		tableNames shouldContainAll listOf("authors", "books", "book_authors")
	}

	test("Flyway creates required constraints") {
		val constraintNames = dsl
			.fetch(
				"""
				select constraint_name
				from information_schema.table_constraints
				where table_schema = 'public'
				""".trimIndent(),
			)
			.map { it.get("constraint_name", String::class.java) }

		constraintNames shouldContainAll listOf(
			"pk_authors",
			"chk_authors_birth_date",
			"pk_books",
			"chk_books_price",
			"chk_books_publication_status",
			"pk_book_authors",
			"fk_book_authors_book_id",
			"fk_book_authors_author_id",
		)
	}

	test("Flyway creates an index for looking up books by author") {
		val indexExists = dsl
			.fetchOne(
				"""
				select exists (
				  select 1
				  from pg_indexes
				  where schemaname = 'public'
				    and tablename = 'book_authors'
				    and indexname = 'idx_book_authors_author_id'
				) as exists
				""".trimIndent(),
			)
			?.get("exists", Boolean::class.java)

		indexExists shouldBe true
	}
}) {
	@Autowired
	lateinit var dsl: DSLContext
}
