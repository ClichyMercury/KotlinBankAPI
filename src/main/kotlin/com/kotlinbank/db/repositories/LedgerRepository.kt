package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.LedgerTable
import com.kotlinbank.models.LedgerEntry
import com.kotlinbank.models.LedgerType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.util.UUID

object LedgerRepository {

    suspend fun create(
        userId: UUID,
        type: LedgerType,
        amount: BigDecimal,
        balanceAfter: BigDecimal,
        orderId: UUID? = null
    ): LedgerEntry = newSuspendedTransaction(Dispatchers.IO) {
        val id = UUID.randomUUID()
        LedgerTable.insert {
            it[LedgerTable.id] = id
            it[LedgerTable.userId] = userId
            it[LedgerTable.type] = type
            it[LedgerTable.amount] = amount
            it[LedgerTable.orderId] = orderId
            it[LedgerTable.balanceAfter] = balanceAfter
        }
        LedgerTable.select { LedgerTable.id eq id }.single().toEntry()
    }

    suspend fun findByUserId(userId: UUID, limit: Int = 50): List<LedgerEntry> =
        newSuspendedTransaction(Dispatchers.IO) {
            LedgerTable.select { LedgerTable.userId eq userId }
                .orderBy(LedgerTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toEntry() }
        }

    private fun ResultRow.toEntry(): LedgerEntry = LedgerEntry(
        id = this[LedgerTable.id].value,
        userId = this[LedgerTable.userId].value,
        type = this[LedgerTable.type],
        amount = this[LedgerTable.amount],
        orderId = this[LedgerTable.orderId]?.value,
        balanceAfter = this[LedgerTable.balanceAfter],
        createdAt = this[LedgerTable.createdAt]
    )
}
