package com.kotlinbank.models

import java.time.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = !expiresAt.isAfter(now)
    val isRevoked: Boolean get() = revokedAt != null
}
