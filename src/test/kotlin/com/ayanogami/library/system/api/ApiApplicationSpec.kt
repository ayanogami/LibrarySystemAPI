package com.ayanogami.library.system.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ApiApplicationSpec : FunSpec({
	test("Kotest is configured") {
		"LibrarySystemAPI" shouldBe "LibrarySystemAPI"
	}
})
