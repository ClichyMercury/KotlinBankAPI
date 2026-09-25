package com.kotlinbank.plugins

import com.kotlinbank.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import org.slf4j.LoggerFactory

private val corsLog = LoggerFactory.getLogger("CORS")

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        if (AppConfig.isProduction) {
            val hosts = AppConfig.Cors.allowedHosts
            if (hosts.isEmpty()) {
                corsLog.info("CORS_ALLOWED_HOSTS is empty, no browser origin allowed (fine for a native mobile client)")
            } else {
                corsLog.info("CORS allows ${hosts.joinToString(", ")} over https")
            }
            hosts.forEach { allowHost(it, schemes = listOf("https")) }
        } else {
            allowHost("10.0.2.2:8080")
            allowHost("localhost:8080")
            allowHost("localhost:3000")
            anyHost()
        }
    }
}
