package com.kotlinbank.db.tables

import com.kotlinbank.models.LedgerType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object LedgerTable : UUIDTable("ledger") {
    val userId = reference("user_id", UsersTable)
    val type = enumerationByName("type", 20, LedgerType::class)
    val amount = decimal("amount", 20, 2)
    val orderId = reference("order_id", OrdersTable).nullable()
    val balanceAfter = decimal("balance_after", 20, 2)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
