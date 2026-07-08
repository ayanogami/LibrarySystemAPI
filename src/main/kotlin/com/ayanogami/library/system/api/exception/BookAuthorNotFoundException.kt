package com.ayanogami.library.system.api.exception

class BookAuthorNotFoundException(authorIds: Collection<Long>) :
	RuntimeException("author not found: ${authorIds.joinToString(", ")}")
