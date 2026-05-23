package com.kotlinbank.models.dto

import com.kotlinbank.models.Order
import com.kotlinbank.models.OrderStatus
import com.kotlinbank.models.OrderType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Serializable
data class BuyOrderRequest(
    @Serializable(with = UUIDSerializer::class) val assetId: UUID,
    @Serializable(with = BigDecimalSerializer::class) val quantity: BigDecimal
)

@Serializable
data class OrderResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    @Serializable(with = UUIDSerializer::class) val assetId: UUID,
    val type: OrderType,
    @Serializable(with = BigDecimalSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val total: BigDecimal,
    val status: OrderStatus,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val executedAt: Instant? = null
)

fun Order.toResponse(): OrderResponse = OrderResponse(
    id = id,
    assetId = assetId,
    type = type,
    quantity = quantity,
    price = price,
    total = quantity.multiply(price),
    status = status,
    createdAt = createdAt,
    executedAt = executedAt
)
