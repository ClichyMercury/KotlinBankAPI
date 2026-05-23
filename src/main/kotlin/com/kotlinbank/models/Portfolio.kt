package com.kotlinbank.models

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Portfolio(
    val id: UUID,
    val userId: UUID,
    val balanceFictif: BigDecimal,
    val createdAt: Instant
)

data class PortfolioAsset(
    val id: UUID,
    val portfolioId: UUID,
    val assetId: UUID,
    val quantity: BigDecimal,
    val avgBuyPrice: BigDecimal,
    val updatedAt: Instant
)
