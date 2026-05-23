package com.kotlinbank.models.dto

import com.kotlinbank.models.Asset
import com.kotlinbank.models.AssetType
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Serializable
data class AssetResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val ticker: String,
    val name: String,
    val type: AssetType,
    val market: String,
    @Serializable(with = BigDecimalSerializer::class) val lastPrice: BigDecimal? = null,
    @Serializable(with = InstantSerializer::class) val lastPriceUpdatedAt: Instant? = null
)

fun Asset.toResponse(): AssetResponse = AssetResponse(
    id = id,
    ticker = ticker,
    name = name,
    type = type,
    market = market,
    lastPrice = lastPrice,
    lastPriceUpdatedAt = lastPriceUpdatedAt
)
