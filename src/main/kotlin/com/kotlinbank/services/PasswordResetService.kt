package com.kotlinbank.services

import com.kotlinbank.config.AppConfig
import com.kotlinbank.db.repositories.PasswordResetTokenRepository
import com.kotlinbank.db.repositories.UserRepository
import com.kotlinbank.services.mail.MailService
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

object PasswordResetService {

    private const val TOKEN_BYTES = 32

    private val random = SecureRandom()
    private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    suspend fun request(email: String) {
        val user = UserRepository.findByEmail(email) ?: return
        if (!user.isActive) return

        PasswordResetTokenRepository.invalidateAllForUser(user.id)

        val raw = generateRawToken()
        val expiresAt = Instant.now().plusSeconds(AppConfig.PasswordReset.expirationMinutes * 60)
        PasswordResetTokenRepository.create(user.id, hash(raw), expiresAt)
        MailService.sendPasswordReset(user.email, raw)
    }

    suspend fun consume(rawToken: String): UUID {
        if (rawToken.isBlank()) throw ValidationException("Reset token is required")
        val stored = PasswordResetTokenRepository.findByHash(hash(rawToken))
            ?: throw UnauthorizedException("Invalid or expired reset token")

        if (stored.isUsed || stored.isExpired()) {
            throw UnauthorizedException("Invalid or expired reset token")
        }
        if (!PasswordResetTokenRepository.markUsed(stored.id)) {
            throw UnauthorizedException("Invalid or expired reset token")
        }
        return stored.userId
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(TOKEN_BYTES).also(random::nextBytes)
        return encoder.encodeToString(bytes)
    }

    private fun hash(rawToken: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(rawToken.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
