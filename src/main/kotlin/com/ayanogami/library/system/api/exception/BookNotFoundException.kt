package com.ayanogami.library.system.api.exception

class BookNotFoundException(bookId: Long) : RuntimeException("book not found: $bookId")
