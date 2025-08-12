package com.kotlinbank.plugins

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

        // Pour ton émulateur Android
        allowHost("10.0.2.2:8080")
        allowHost("localhost:8080")

        // En développement
        anyHost()
    }
    println("✅ CORS configuré")
}