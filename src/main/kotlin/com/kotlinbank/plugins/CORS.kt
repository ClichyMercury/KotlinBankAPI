package com.kotlinbank.plugins

import com.kotlinbank.config.AppConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)

        if (AppConfig.environment == "production") {
            allowHost("api.finsim.example", schemes = listOf("https"))
        } else {
            allowHost("10.0.2.2:8080")
            allowHost("localhost:8080")
            allowHost("localhost:3000")
            anyHost()
        }
    }
}
