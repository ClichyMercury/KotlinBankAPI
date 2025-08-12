package com.kotlinbank.plugins

import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
            setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        }
    }
    println("✅ Serialization configurée")
}