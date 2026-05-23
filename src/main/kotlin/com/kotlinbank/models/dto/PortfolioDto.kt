package com.kotlinbank.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.UUID

@Serializable
data class PortfolioResponse(
    @Serializable(with = BigDecimalSerializer::class) val balanceFictif: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val assetsValue: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val totalValue: BigDecimal,
    val assets: List<PortfolioAssetResponse>
)

@Serializable
data class PortfolioAssetResponse(
    @Serializable(with = UUIDSerializer::class) val assetId: UUID,
    val ticker: String,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val avgBuyPrice: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val currentPrice: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class) val currentValue: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val unrealizedPnl: BigDecimal
)
