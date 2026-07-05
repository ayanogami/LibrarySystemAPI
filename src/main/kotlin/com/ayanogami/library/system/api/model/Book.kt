package com.ayanogami.library.system.api.model

import com.ayanogami.library.system.api.model.Author

data class Book(
	val id: Long,
	val title: String,
	val price: Int,
	val publicationStatus: PublicationStatus,
	val authors: List<Author>,
)
