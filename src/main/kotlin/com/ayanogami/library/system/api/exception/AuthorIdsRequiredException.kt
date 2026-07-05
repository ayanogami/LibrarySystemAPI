package com.ayanogami.library.system.api.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class AuthorIdsRequiredException : RuntimeException("authorIds is required")
