package com.kotlinbank.services.monitoring

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SentryReporterTest {

    private val captured = mutableListOf<SentryEvent>()

    private fun initWithCapture() {
        Sentry.init { options ->
            options.dsn = "https://publickey@o0.ingest.sentry.io/0"
            options.setBeforeSend { event, _ ->
                captured += event
                null
            }
        }
    }

    @AfterEach
    fun tearDown() {
        Sentry.close()
        captured.clear()
    }

    @Test
    fun `scrubUrl drops the query string carrying a reset token`() {
        assertEquals("/reset-password", SentryReporter.scrubUrl("/reset-password?token=super-secret"))
        assertEquals("/reset-password", SentryReporter.scrubUrl("/reset-password#token=super-secret"))
        assertEquals("/api/v1/portfolio", SentryReporter.scrubUrl("/api/v1/portfolio"))
    }

    @Test
    fun `request context keeps the path but never the token`() {
        val context = SentryReporter.requestContext("GET", "/reset-password?token=super-secret", null)

        assertEquals("GET", context["http.method"])
        assertEquals("/reset-password", context["http.path"])
        assertNull(context["user.id"])
        assertTrue(context.values.none { it.contains("super-secret") }, "the token must not reach Sentry")
    }

    @Test
    fun `request context carries the user id when authenticated`() {
        val context = SentryReporter.requestContext("POST", "/api/v1/orders/buy", "user-42")

        assertEquals("user-42", context["user.id"])
    }

    @Test
    fun `capture is a no-op when sentry is disabled`() {
        assertDoesNotThrow {
            SentryReporter.captureException(IllegalStateException("boom"))
            SentryReporter.captureMessage("nothing to see", SentryLevel.ERROR)
        }
        assertTrue(captured.isEmpty())
    }

    @Test
    fun `captured exception carries the request tags`() {
        initWithCapture()

        SentryReporter.captureException(
            IllegalStateException("boom"),
            SentryReporter.requestContext("GET", "/reset-password?token=super-secret", "user-42")
        )

        val event = captured.single()
        assertEquals("boom", event.throwable?.message)
        assertEquals("/reset-password", event.tags?.get("http.path"))
        assertEquals("user-42", event.tags?.get("user.id"))
        assertTrue(event.tags?.values?.none { it.contains("super-secret") } ?: true)
    }

    @Test
    fun `captured message carries its level and tags`() {
        initWithCapture()

        SentryReporter.captureMessage("prices are stale", SentryLevel.ERROR, mapOf("job" to "price_refresh"))

        val event = captured.single()
        assertEquals("prices are stale", event.message?.formatted)
        assertEquals(SentryLevel.ERROR, event.level)
        assertEquals("price_refresh", event.tags?.get("job"))
    }
}
