package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.PublicationStatus

data class UpdateBookRequest(
	val title: String?,

	val price: Int?,

	val authorIds: List<Long>?,

	val publicationStatus: PublicationStatus?,
)
