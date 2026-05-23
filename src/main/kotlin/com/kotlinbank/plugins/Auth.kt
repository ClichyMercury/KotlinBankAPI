package com.kotlinbank.plugins

import com.kotlinbank.config.AppConfig
import com.kotlinbank.models.dto.ErrorResponse
import com.kotlinbank.services.JwtService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

const val AUTH_JWT = "auth-jwt"

fun Application.configureAuth() {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = AppConfig.Jwt.realm
            verifier(JwtService.verifier)
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("unauthorized", "Missing or invalid token")
                )
            }
        }
    }
}
