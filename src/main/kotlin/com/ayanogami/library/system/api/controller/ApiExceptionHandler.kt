package com.ayanogami.library.system.api.controller

import com.ayanogami.library.system.api.exception.AuthorIdsRequiredException
import com.ayanogami.library.system.api.exception.AuthorNotFoundException
import com.ayanogami.library.system.api.exception.BookAuthorNotFoundException
import com.ayanogami.library.system.api.exception.BookNotFoundException
import com.ayanogami.library.system.api.exception.InvalidAuthorException
import com.ayanogami.library.system.api.exception.InvalidBookException
import com.ayanogami.library.system.api.view.ErrorDetailResponse
import com.ayanogami.library.system.api.view.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {
	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
		ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(
				ErrorResponse(
					code = "VALIDATION_ERROR",
					message = "validation failed",
					details = exception.bindingResult.fieldErrors.map { fieldError ->
						ErrorDetailResponse(
							field = fieldError.field,
							message = fieldError.defaultMessage ?: "invalid value",
						)
					},
				),
			)

	@ExceptionHandler(
		AuthorIdsRequiredException::class,
		BookAuthorNotFoundException::class,
		InvalidAuthorException::class,
		InvalidBookException::class,
	)
	fun handleBadRequest(exception: RuntimeException): ResponseEntity<ErrorResponse> =
		errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.message ?: "bad request")

	@ExceptionHandler(
		AuthorNotFoundException::class,
		BookNotFoundException::class,
	)
	fun handleNotFound(exception: RuntimeException): ResponseEntity<ErrorResponse> =
		errorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.message ?: "not found")

	@ExceptionHandler(MissingServletRequestParameterException::class)
	fun handleMissingRequestParameter(
		exception: MissingServletRequestParameterException,
	): ResponseEntity<ErrorResponse> =
		errorResponse(
			HttpStatus.BAD_REQUEST,
			"BAD_REQUEST",
			"${exception.parameterName} is required",
		)

	@ExceptionHandler(HttpMessageNotReadableException::class)
	fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
		errorResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "request body is invalid")

	private fun errorResponse(
		status: HttpStatus,
		code: String,
		message: String,
	): ResponseEntity<ErrorResponse> =
		ResponseEntity
			.status(status)
			.body(ErrorResponse(code = code, message = message))
}
