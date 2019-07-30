package io.kay

import io.kay.service.ConditionsRepository
import io.kay.web.mainWithDependencies
import io.kay.web.module
import io.ktor.application.Application
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

    val repo = mockk<ConditionsRepository>()

    @Test
    fun emptyConditionsListReturns404() = httpTest{
        every {repo.findConditionsByUser(any())} returns listOf()

        with(handleRequest(HttpMethod.Get, "/rest/user/conditions")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }

    private fun httpTest(callback: suspend TestApplicationEngine.() -> Unit): Unit {
        withTestApplication({mainWithDependencies(repo)}) {
            runBlocking { callback() }
        }
    }
}
