package com.ayanogami.library.system.api.view

data class ErrorResponse(
	val code: String,
	val message: String,
	val details: List<ErrorDetailResponse> = emptyList(),
)
