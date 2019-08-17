package io.kay

import io.kay.service.ConditionsRepository
import io.kay.service.LogRepository
import io.kay.service.UserRepository
import io.kay.web.mainWithDependencies
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class TaimaTest {

    val conditionsRepository = mockk<ConditionsRepository>()
    val userRepository = mockk<UserRepository>()
    val logRepository = mockk<LogRepository>()

    @Test
    fun emptyConditionsListReturns404() = httpTest {
        every { conditionsRepository.findConditionsByUser(any()) } returns listOf()

        with(handleRequest(HttpMethod.Get, "/rest/user/conditions")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    private fun httpTest(callback: suspend TestApplicationEngine.() -> Unit) {
        withTestApplication({ mainWithDependencies(conditionsRepository, userRepository, logRepository) }) {
            runBlocking { callback() }
        }
    }
}
