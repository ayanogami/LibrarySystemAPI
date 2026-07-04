package com.ayanogami.library.system.api.author.view

import java.time.LocalDate

data class CreateAuthorRequest(
	val name: String,
	val birthDate: LocalDate,
)
