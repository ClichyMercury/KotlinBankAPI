package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.RefreshTokensTable
import com.kotlinbank.models.RefreshToken
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

object RefreshTokenRepository {

    suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant): RefreshToken =
        newSuspendedTransaction(Dispatchers.IO) {
            val id = UUID.randomUUID()
            RefreshTokensTable.insert {
                it[RefreshTokensTable.id] = id
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt
            }
            RefreshTokensTable.select { RefreshTokensTable.id eq id }.single().toRefreshToken()
        }

    suspend fun findByHash(tokenHash: String): RefreshToken? = newSuspendedTransaction(Dispatchers.IO) {
        RefreshTokensTable.select { RefreshTokensTable.tokenHash eq tokenHash }
            .singleOrNull()
            ?.toRefreshToken()
    }

    suspend fun revoke(id: UUID, now: Instant = Instant.now()): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.update({
                (RefreshTokensTable.id eq id) and RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = now
            } > 0
        }

    suspend fun revokeAllForUser(userId: UUID, now: Instant = Instant.now()): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.update({
                (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = now
            }
        }

    suspend fun deleteByHash(tokenHash: String): Int = newSuspendedTransaction(Dispatchers.IO) {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.tokenHash.eq(tokenHash) }
    }

    suspend fun deleteAllForUser(userId: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        RefreshTokensTable.deleteWhere { RefreshTokensTable.userId.eq(userId) }
    }

    suspend fun deleteExpired(before: Instant = Instant.now()): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            RefreshTokensTable.deleteWhere { expiresAt less before }
        }

    private fun ResultRow.toRefreshToken(): RefreshToken = RefreshToken(
        id = this[RefreshTokensTable.id].value,
        userId = this[RefreshTokensTable.userId].value,
        tokenHash = this[RefreshTokensTable.tokenHash],
        expiresAt = this[RefreshTokensTable.expiresAt],
        revokedAt = this[RefreshTokensTable.revokedAt],
        createdAt = this[RefreshTokensTable.createdAt]
    )
}
