package com.kotlinbank.db.tables

import com.kotlinbank.models.OrderStatus
import com.kotlinbank.models.OrderType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object OrdersTable : UUIDTable("orders") {
    val userId = reference("user_id", UsersTable)
    val assetId = reference("asset_id", AssetsTable)
    val type = enumerationByName("type", 10, OrderType::class)
    val quantity = decimal("quantity", 20, 8)
    val price = decimal("price", 20, 8)
    val status = enumerationByName("status", 20, OrderStatus::class).default(OrderStatus.PENDING)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val executedAt = timestamp("executed_at").nullable()
}
