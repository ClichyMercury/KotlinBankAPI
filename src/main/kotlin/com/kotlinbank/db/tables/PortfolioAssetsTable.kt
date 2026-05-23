package com.kotlinbank.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant

object PortfolioAssetsTable : UUIDTable("portfolio_assets") {
    val portfolioId = reference("portfolio_id", PortfoliosTable)
    val assetId = reference("asset_id", AssetsTable)
    val quantity = decimal("quantity", 20, 8).default(BigDecimal.ZERO)
    val avgBuyPrice = decimal("avg_buy_price", 20, 8).default(BigDecimal.ZERO)
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }

    init {
        uniqueIndex(portfolioId, assetId)
    }
}
