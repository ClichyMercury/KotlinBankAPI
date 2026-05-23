package com.kotlinbank.plugins

import com.kotlinbank.config.RedisFactory
import com.kotlinbank.models.dto.HealthResponse
import com.kotlinbank.routes.authRoutes
import com.kotlinbank.routes.marketRoutes
import com.kotlinbank.routes.orderRoutes
import com.kotlinbank.routes.portfolioRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText(
                """
                FinSim API — slice J1 OK
                GET /health — liveness check (DB + Redis)
                """.trimIndent()
            )
        }

        get("/health") {
            val dbOk = runCatching { transaction { exec("SELECT 1") {} } }.isSuccess
            val redisOk = runCatching { RedisFactory.sync().ping() == "PONG" }.getOrDefault(false)
            val healthy = dbOk && redisOk
            val httpStatus = if (healthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(
                httpStatus,
                HealthResponse(
                    status = if (healthy) "ok" else "degraded",
                    db = dbOk,
                    redis = redisOk
                )
            )
        }

        authRoutes()
        marketRoutes()
        portfolioRoutes()
        orderRoutes()
    }
}
