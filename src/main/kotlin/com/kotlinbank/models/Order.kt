package com.kotlinbank.models

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class OrderType { BUY, SELL }
enum class OrderStatus { PENDING, EXECUTED, FAILED }

data class Order(
    val id: UUID,
    val userId: UUID,
    val assetId: UUID,
    val type: OrderType,
    val quantity: BigDecimal,
    val price: BigDecimal,
    val status: OrderStatus,
    val createdAt: Instant,
    val executedAt: Instant?
)
