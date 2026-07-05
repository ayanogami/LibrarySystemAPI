package com.ayanogami.library.system.api.controller

import com.ayanogami.library.system.api.service.BookService
import com.ayanogami.library.system.api.view.BookResponse
import com.ayanogami.library.system.api.view.CreateBookRequest
import com.ayanogami.library.system.api.view.UpdateBookRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/books")
class BookController(
	private val bookService: BookService,
) {
	@PostMapping
	fun create(@Valid @RequestBody request: CreateBookRequest): ResponseEntity<BookResponse> {
		val book = bookService.create(
			title = request.title,
			price = request.price,
			authorIds = request.authorIds,
			publicationStatus = request.publicationStatus,
		)

		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(BookResponse.from(book))
	}

	@PutMapping("/{bookId}")
	fun update(
		@PathVariable bookId: Long,
		@Valid @RequestBody request: UpdateBookRequest,
	): ResponseEntity<BookResponse> {
		val book = bookService.update(
			id = bookId,
			title = request.title,
			price = request.price,
			authorIds = request.authorIds,
			publicationStatus = request.publicationStatus,
		)

		return ResponseEntity.ok(BookResponse.from(book))
	}
}
