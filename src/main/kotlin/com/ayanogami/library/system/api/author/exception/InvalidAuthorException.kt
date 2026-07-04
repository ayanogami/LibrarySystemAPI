package com.ayanogami.library.system.api.author.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidAuthorException(message: String) : RuntimeException(message)
