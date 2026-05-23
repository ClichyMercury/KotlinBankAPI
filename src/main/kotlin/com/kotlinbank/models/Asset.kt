package com.kotlinbank.models

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class AssetType { STOCK, FOREX, CRYPTO }

data class Asset(
    val id: UUID,
    val ticker: String,
    val name: String,
    val type: AssetType,
    val market: String,
    val externalId: String?,
    val lastPrice: BigDecimal?,
    val lastPriceUpdatedAt: Instant?,
    val createdAt: Instant
)
