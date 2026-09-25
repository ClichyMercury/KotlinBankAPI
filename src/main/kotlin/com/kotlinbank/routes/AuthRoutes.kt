package com.kotlinbank.routes

import com.kotlinbank.models.dto.ForgotPasswordRequest
import com.kotlinbank.models.dto.LoginRequest
import com.kotlinbank.models.dto.MessageResponse
import com.kotlinbank.models.dto.RefreshRequest
import com.kotlinbank.models.dto.RegisterRequest
import com.kotlinbank.models.dto.ResetPasswordRequest
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

            post("/refresh") {
                val req = call.receive<RefreshRequest>()
                call.respond(HttpStatusCode.OK, AuthService.refresh(req))
            }

            post("/forgot-password") {
                val req = call.receive<ForgotPasswordRequest>()
                AuthService.forgotPassword(req)
                call.respond(
                    HttpStatusCode.OK,
                    MessageResponse("If that email is registered, a reset link has been sent")
                )
            }

            post("/reset-password") {
                val req = call.receive<ResetPasswordRequest>()
                AuthService.resetPassword(req)
                call.respond(HttpStatusCode.OK, MessageResponse("Password updated, all sessions revoked"))
            }

            post("/logout") {
                val req = call.receive<RefreshRequest>()
                AuthService.logout(req)
                call.respond(HttpStatusCode.OK, MessageResponse("Logged out"))
            }
        }

        authenticate(AUTH_JWT) {
            get("/me") {
                val userId = call.userId()
                call.respond(AuthService.me(userId))
            }

            post("/logout-all") {
                AuthService.logoutAll(call.userId())
                call.respond(HttpStatusCode.OK, MessageResponse("All sessions revoked"))
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
