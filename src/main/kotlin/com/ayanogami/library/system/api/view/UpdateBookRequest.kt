package com.ayanogami.library.system.api.view

import com.ayanogami.library.system.api.model.PublicationStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero

data class UpdateBookRequest(
	@field:NotBlank
	val title: String,

	@field:PositiveOrZero
	val price: Int,

	@field:NotEmpty
	val authorIds: List<Long>,

	val publicationStatus: PublicationStatus,
)
