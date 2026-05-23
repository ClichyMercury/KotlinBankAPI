package com.kotlinbank.services

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.kotlinbank.config.AppConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

object JwtService {

    private val algorithm: Algorithm = Algorithm.HMAC256(AppConfig.Jwt.secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(AppConfig.Jwt.issuer)
        .withAudience(AppConfig.Jwt.audience)
        .build()

    fun issue(userId: UUID, email: String): IssuedToken {
        val now = Instant.now()
        val exp = now.plusSeconds(AppConfig.Jwt.expirationMinutes * 60)
        val token = JWT.create()
            .withIssuer(AppConfig.Jwt.issuer)
            .withAudience(AppConfig.Jwt.audience)
            .withClaim("userId", userId.toString())
            .withClaim("email", email)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(exp))
            .sign(algorithm)
        return IssuedToken(token, exp.epochSecond - now.epochSecond)
    }

    data class IssuedToken(val token: String, val expiresInSeconds: Long)
}
