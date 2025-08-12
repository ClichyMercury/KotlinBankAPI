package com.kotlinbank

import com.kotlinbank.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    println("🚀 Démarrage de KotlinBank API...")
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureCORS()
    configureRouting()

    // Log de démarrage
    println("✅ KotlinBank API démarrée sur http://localhost:8080")
    println("📱 Endpoints disponibles :")
    println("   GET http://localhost:8080/")
    println("   GET http://localhost:8080/api/transactions")
    println("   GET http://localhost:8080/api/cards")
    println("   GET http://localhost:8080/api/notifications")
}