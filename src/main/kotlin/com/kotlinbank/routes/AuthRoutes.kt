package com.kotlinbank.routes

import com.kotlinbank.models.dto.LoginRequest
import com.kotlinbank.models.dto.RegisterRequest
import com.kotlinbank.plugins.AUTH_JWT
import com.kotlinbank.plugins.RATE_LIMIT_AUTH
import com.kotlinbank.services.AuthService
import com.kotlinbank.services.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.authRoutes() {
    route("/api/v1/auth") {

        rateLimit(RATE_LIMIT_AUTH) {
            post("/register") {
                val req = call.receive<RegisterRequest>()
                val response = AuthService.register(req)
                call.respond(HttpStatusCode.Created, response)
            }

            post("/login") {
                val req = call.receive<LoginRequest>()
                val response = AuthService.login(req)
                call.respond(HttpStatusCode.OK, response)
            }
        }

        authenticate(AUTH_JWT) {
            get("/me") {
                val userId = call.userId()
                call.respond(AuthService.me(userId))
            }
        }
    }
}

fun ApplicationCall.userId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw UnauthorizedException("Missing token")
    val claim = principal.payload.getClaim("userId").asString()
        ?: throw UnauthorizedException("Invalid token claims")
    return UUID.fromString(claim)
}
