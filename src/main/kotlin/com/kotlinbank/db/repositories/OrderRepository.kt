package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.OrdersTable
import com.kotlinbank.models.Order
import com.kotlinbank.models.OrderStatus
import com.kotlinbank.models.OrderType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object OrderRepository {

    suspend fun create(
        userId: UUID,
        assetId: UUID,
        type: OrderType,
        quantity: BigDecimal,
        price: BigDecimal,
        status: OrderStatus,
        executedAt: Instant?
    ): Order = newSuspendedTransaction(Dispatchers.IO) {
        val id = UUID.randomUUID()
        OrdersTable.insert {
            it[OrdersTable.id] = id
            it[OrdersTable.userId] = userId
            it[OrdersTable.assetId] = assetId
            it[OrdersTable.type] = type
            it[OrdersTable.quantity] = quantity
            it[OrdersTable.price] = price
            it[OrdersTable.status] = status
            it[OrdersTable.executedAt] = executedAt
        }
        OrdersTable.select { OrdersTable.id eq id }.single().toOrder()
    }

    suspend fun findById(id: UUID): Order? = newSuspendedTransaction(Dispatchers.IO) {
        OrdersTable.select { OrdersTable.id eq id }.singleOrNull()?.toOrder()
    }

    suspend fun findByUserId(userId: UUID, limit: Int = 50): List<Order> =
        newSuspendedTransaction(Dispatchers.IO) {
            OrdersTable.select { OrdersTable.userId eq userId }
                .orderBy(OrdersTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toOrder() }
        }

    private fun ResultRow.toOrder(): Order = Order(
        id = this[OrdersTable.id].value,
        userId = this[OrdersTable.userId].value,
        assetId = this[OrdersTable.assetId].value,
        type = this[OrdersTable.type],
        quantity = this[OrdersTable.quantity],
        price = this[OrdersTable.price],
        status = this[OrdersTable.status],
        createdAt = this[OrdersTable.createdAt],
        executedAt = this[OrdersTable.executedAt]
    )
}
