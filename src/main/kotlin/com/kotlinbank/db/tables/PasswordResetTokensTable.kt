package com.kotlinbank.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object PasswordResetTokensTable : UUIDTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable)
    val tokenHash = char("token_hash", 64).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
