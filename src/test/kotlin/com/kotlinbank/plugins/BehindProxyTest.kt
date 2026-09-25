package com.kotlinbank.plugins

import com.kotlinbank.config.AppConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BehindProxyTest {

    @Test
    fun `without the plugin the scheme stays the one of the proxy hop`() = testApplication {
        application {
            routing {
                get("/origin") {
                    call.respondText("${call.request.origin.scheme}|${call.request.origin.remoteHost}")
                }
            }
        }

        val body = client.get("/origin") {
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-For", "41.82.10.7")
        }.bodyAsText()

        assertEquals("http", body.substringBefore('|'), "the app believes the request is plain http")
    }

    @Test
    fun `with the plugin the real scheme and client ip come through`() = testApplication {
        application {
            this.install(XForwardedHeaders)
            routing {
                get("/origin") {
                    call.respondText("${call.request.origin.scheme}|${call.request.origin.remoteHost}")
                }
            }
        }

        val body = client.get("/origin") {
            header("X-Forwarded-Proto", "https")
            header("X-Forwarded-For", "41.82.10.7")
        }.bodyAsText()

        assertEquals("https", body.substringBefore('|'), "https is what CORS compares against the Origin header")
        assertEquals("41.82.10.7", body.substringAfter('|'), "the rate limit keys on the real client, not the proxy")
    }

    @Test
    fun `the host serving the reset page is always a permitted origin`() {
        val host = AppConfig.hostOf(AppConfig.Mail.resetUrlTemplate)

        assertTrue(host != null, "the reset url must be an https url we can derive a host from")
        assertTrue(
            AppConfig.Cors.allowedHosts.contains(host),
            "the page we serve ourselves must never be rejected by our own CORS rules"
        )
    }
}
