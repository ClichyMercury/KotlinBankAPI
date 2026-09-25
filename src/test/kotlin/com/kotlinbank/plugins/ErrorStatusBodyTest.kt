package com.kotlinbank.plugins

import com.kotlinbank.models.dto.ErrorResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ErrorStatusBodyTest {

    @Test
    fun `a bare 429 is given a json body`() = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            routing { get("/boom") { call.respond(HttpStatusCode.TooManyRequests) } }
        }
        val http = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val response = http.get("/boom")

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        val body = response.body<ErrorResponse>()
        assertEquals("rate_limited", body.error)
        assertTrue(body.message.isNotBlank())
    }

    @Test
    fun `a bare 403 is given a json body`() = testApplication {
        application {
            configureSerialization()
            configureStatusPages()
            routing { get("/boom") { call.respond(HttpStatusCode.Forbidden) } }
        }
        val http = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

        val body = http.get("/boom").body<ErrorResponse>()

        assertEquals("forbidden", body.error)
    }
}
