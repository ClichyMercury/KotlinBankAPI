package com.kotlinbank.services

import com.kotlinbank.config.AppConfig
import com.kotlinbank.db.repositories.RefreshTokenRepository
import com.kotlinbank.models.RefreshToken
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

object RefreshTokenService {

    private const val TOKEN_BYTES = 32

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    suspend fun issue(userId: UUID): IssuedRefreshToken {
        val raw = generateRawToken()
        val now = Instant.now()
        val expiresAt = now.plusSeconds(AppConfig.Jwt.refreshExpirationDays * 24 * 3600)
        RefreshTokenRepository.create(userId, hash(raw), expiresAt)
        return IssuedRefreshToken(raw, expiresAt.epochSecond - now.epochSecond)
    }

    suspend fun consume(rawToken: String): UUID {
        if (rawToken.isBlank()) throw ValidationException("Refresh token is required")
        val stored = RefreshTokenRepository.findByHash(hash(rawToken))
            ?: throw UnauthorizedException("Invalid refresh token")

        if (stored.isRevoked) {
            RefreshTokenRepository.revokeAllForUser(stored.userId)
            throw UnauthorizedException("Refresh token reuse detected, all sessions revoked")
        }
        if (stored.isExpired()) throw UnauthorizedException("Refresh token expired")
        if (!RefreshTokenRepository.revoke(stored.id)) {
            throw UnauthorizedException("Invalid refresh token")
        }
        return stored.userId
    }

    suspend fun revoke(rawToken: String) {
        if (rawToken.isBlank()) throw ValidationException("Refresh token is required")
        val stored: RefreshToken = RefreshTokenRepository.findByHash(hash(rawToken)) ?: return
        RefreshTokenRepository.revoke(stored.id)
    }

    suspend fun revokeAllForUser(userId: UUID): Int = RefreshTokenRepository.revokeAllForUser(userId)

    private fun generateRawToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return encoder.encodeToString(bytes)
    }

    private fun hash(rawToken: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray())
            .joinToString("") { "%02x".format(it) }

    data class IssuedRefreshToken(val token: String, val expiresInSeconds: Long)
}
