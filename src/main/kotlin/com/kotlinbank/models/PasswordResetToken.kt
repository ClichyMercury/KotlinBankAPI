package com.kotlinbank.models

import java.time.Instant
import java.util.UUID

data class PasswordResetToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
    val createdAt: Instant
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = !expiresAt.isAfter(now)
    val isUsed: Boolean get() = usedAt != null
}
