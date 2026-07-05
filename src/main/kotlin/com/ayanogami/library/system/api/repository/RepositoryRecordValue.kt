package com.ayanogami.library.system.api.repository

import com.ayanogami.library.system.api.exception.RepositoryMappingException

fun <T : Any> requireRecordValue(value: T?, name: String): T =
	value ?: throw RepositoryMappingException("$name must not be null")
