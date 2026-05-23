package com.kotlinbank.models

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class LedgerType { DEPOSIT, BUY, SELL, FEE }

data class LedgerEntry(
    val id: UUID,
    val userId: UUID,
    val type: LedgerType,
    val amount: BigDecimal,
    val orderId: UUID?,
    val balanceAfter: BigDecimal,
    val createdAt: Instant
)
