package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.PasswordResetTokensTable
import com.kotlinbank.models.PasswordResetToken
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

object PasswordResetTokenRepository {

    suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant): PasswordResetToken =
        newSuspendedTransaction(Dispatchers.IO) {
            val id = UUID.randomUUID()
            PasswordResetTokensTable.insert {
                it[PasswordResetTokensTable.id] = id
                it[PasswordResetTokensTable.userId] = userId
                it[PasswordResetTokensTable.tokenHash] = tokenHash
                it[PasswordResetTokensTable.expiresAt] = expiresAt
            }
            PasswordResetTokensTable.select { PasswordResetTokensTable.id eq id }.single().toToken()
        }

    suspend fun findByHash(tokenHash: String): PasswordResetToken? =
        newSuspendedTransaction(Dispatchers.IO) {
            PasswordResetTokensTable.select { PasswordResetTokensTable.tokenHash eq tokenHash }
                .singleOrNull()
                ?.toToken()
        }

    suspend fun markUsed(id: UUID, now: Instant = Instant.now()): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            PasswordResetTokensTable.update({
                (PasswordResetTokensTable.id eq id) and PasswordResetTokensTable.usedAt.isNull()
            }) {
                it[usedAt] = now
            } > 0
        }

    suspend fun invalidateAllForUser(userId: UUID, now: Instant = Instant.now()): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            PasswordResetTokensTable.update({
                (PasswordResetTokensTable.userId eq userId) and PasswordResetTokensTable.usedAt.isNull()
            }) {
                it[usedAt] = now
            }
        }

    suspend fun deleteExpired(before: Instant = Instant.now()): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            PasswordResetTokensTable.deleteWhere { expiresAt less before }
        }

    private fun ResultRow.toToken(): PasswordResetToken = PasswordResetToken(
        id = this[PasswordResetTokensTable.id].value,
        userId = this[PasswordResetTokensTable.userId].value,
        tokenHash = this[PasswordResetTokensTable.tokenHash],
        expiresAt = this[PasswordResetTokensTable.expiresAt],
        usedAt = this[PasswordResetTokensTable.usedAt],
        createdAt = this[PasswordResetTokensTable.createdAt]
    )
}
