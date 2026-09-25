package com.kotlinbank.services.mail

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResendMailSenderTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun sender(
        engine: MockEngine,
        from: String = "FinSim <no-reply@finsim.app>",
        resetUrlTemplate: String = "https://finsim.app/reset-password?token={token}"
    ) = ResendMailSender(
        engine = engine,
        apiKey = "re_test_key",
        from = from,
        resetUrlTemplate = resetUrlTemplate
    )

    private fun bodyOf(request: HttpRequestData) =
        Json.parseToJsonElement((request.body as TextContent).text).jsonObject

    @Test
    fun `posts the reset email to the resend api`() {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond("""{"id":"e1"}""", HttpStatusCode.OK, jsonHeaders)
        }

        runBlocking { sender(engine).sendPasswordReset("toi@test.io", "tok-123") }

        assertEquals(1, requests.size)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://api.resend.com/emails", request.url.toString())
        assertEquals("Bearer re_test_key", request.headers[HttpHeaders.Authorization])

        val body = bodyOf(request)
        assertEquals("FinSim <no-reply@finsim.app>", body["from"]!!.jsonPrimitive.content)
        assertEquals("toi@test.io", body["to"]!!.jsonArray.single().jsonPrimitive.content)
        assertTrue(body["subject"]!!.jsonPrimitive.content.isNotBlank())
    }

    @Test
    fun `builds the reset link from the template`() {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond("""{"id":"e1"}""", HttpStatusCode.OK, jsonHeaders)
        }

        runBlocking {
            sender(engine, resetUrlTemplate = "finsim://reset?token={token}")
                .sendPasswordReset("toi@test.io", "tok-123")
        }

        val body = bodyOf(requests.single())
        val expected = "finsim://reset?token=tok-123"
        assertTrue(body["html"]!!.jsonPrimitive.content.contains(expected), "html carries the link")
        assertTrue(body["text"]!!.jsonPrimitive.content.contains(expected), "text carries the link")
    }

    @Test
    fun `never sends the raw token outside the link`() {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            respond("""{"id":"e1"}""", HttpStatusCode.OK, jsonHeaders)
        }

        runBlocking { sender(engine).sendPasswordReset("toi@test.io", "tok-123") }

        val subject = bodyOf(requests.single())["subject"]!!.jsonPrimitive.content
        assertTrue(!subject.contains("tok-123"), "the token must not leak into the subject line")
    }

    @Test
    fun `a resend failure does not bubble up`() {
        val engine = MockEngine { respondError(HttpStatusCode.UnprocessableEntity, """{"message":"domain not verified"}""") }

        assertDoesNotThrow {
            runBlocking { sender(engine).sendPasswordReset("toi@test.io", "tok-123") }
        }
    }

    @Test
    fun `a transport error does not bubble up`() {
        val engine = MockEngine { throw java.io.IOException("connection reset") }

        assertDoesNotThrow {
            runBlocking { sender(engine).sendPasswordReset("toi@test.io", "tok-123") }
        }
    }
}
