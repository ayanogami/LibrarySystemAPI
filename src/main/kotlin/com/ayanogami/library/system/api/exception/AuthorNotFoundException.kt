package com.ayanogami.library.system.api.exception

class AuthorNotFoundException(authorId: Long) : RuntimeException("author not found: $authorId")
