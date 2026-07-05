package com.ayanogami.library.system.api

import com.ayanogami.library.system.api.exception.RepositoryMappingException
import com.ayanogami.library.system.api.repository.requireRecordValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RepositoryRecordValueSpec : DescribeSpec({
	describe("requireRecordValue") {
		context("value が null でない場合") {
			it("value を返す") {
				requireRecordValue("吾輩は猫である", "book title") shouldBe "吾輩は猫である"
			}
		}

		context("value が null の場合") {
			it("RepositoryMappingException を投げる") {
				val exception = shouldThrow<RepositoryMappingException> {
					requireRecordValue(null, "book title")
				}

				exception.message shouldBe "book title must not be null"
			}
		}
	}
})
