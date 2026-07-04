package com.ayanogami.library.system.api.author

import com.ayanogami.library.system.api.author.exception.InvalidAuthorException
import com.ayanogami.library.system.api.author.model.Author
import com.ayanogami.library.system.api.author.repository.AuthorRepository
import com.ayanogami.library.system.api.author.service.AuthorService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class AuthorServiceSpec : DescribeSpec({
	describe("著者作成") {
		context("リクエストが妥当な場合") {
			it("作成された著者を返す") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val birthDate = LocalDate.of(1867, 2, 9)
				every { repository.create("夏目漱石", birthDate) } returns Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)

				val author = service.create("夏目漱石", birthDate)

				author shouldBe Author(
					id = 1,
					name = "夏目漱石",
					birthDate = birthDate,
				)
				verify(exactly = 1) { repository.create("夏目漱石", birthDate) }
			}
		}

		context("生年月日が現在日の場合") {
			it("作成された著者を返す") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val today = LocalDate.now()
				every { repository.create("今日 生まれ", today) } returns Author(
					id = 1,
					name = "今日 生まれ",
					birthDate = today,
				)

				val author = service.create("今日 生まれ", today)

				author.birthDate shouldBe today
				verify(exactly = 1) { repository.create("今日 生まれ", today) }
			}
		}

		context("生年月日が現在日より後の場合") {
			it("InvalidAuthorException を投げる") {
				val repository = mockk<AuthorRepository>()
				val service = AuthorService(repository)
				val tomorrow = LocalDate.now().plusDays(1)

				val exception = shouldThrow<InvalidAuthorException> {
					service.create("未来 太郎", tomorrow)
				}

				exception.message shouldBe "birthDate must be today or earlier"
				verify(exactly = 0) { repository.create(any(), any()) }
			}
		}
	}
})
