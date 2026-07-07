package com.ayanogami.library.system.api.model

data class AuthorBooks(
	val author: Author,
	val books: List<Book>,
)
