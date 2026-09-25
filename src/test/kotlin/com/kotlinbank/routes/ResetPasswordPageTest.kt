package com.kotlinbank.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetPasswordPageTest {

    private fun Application.pageOnly() = routing { webRoutes() }

    @Test
    fun `serves the reset page as html`() = testApplication {
        application { pageOnly() }

        val response = client.get("/reset-password?token=tok-123")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Text.Html, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsText().contains("<form"), "the page carries the form")
    }

    @Test
    fun `never echoes the token into the markup`() = testApplication {
        application { pageOnly() }

        val body = client.get("/reset-password?token=leaky-token-value").bodyAsText()

        assertTrue(
            !body.contains("leaky-token-value"),
            "the token is read from location.search by the browser, never injected server side"
        )
    }

    @Test
    fun `is served without caching`() = testApplication {
        application { pageOnly() }

        val response = client.get("/reset-password?token=tok-123")

        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
    }

    @Test
    fun `posts to the reset endpoint`() = testApplication {
        application { pageOnly() }

        val body = client.get("/reset-password").bodyAsText()

        assertTrue(body.contains("/api/v1/auth/reset-password"), "the form targets the reset endpoint")
    }
}
