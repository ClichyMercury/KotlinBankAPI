package com.kotlinbank.services

import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.db.repositories.PortfolioRepository
import com.kotlinbank.models.dto.PortfolioAssetResponse
import com.kotlinbank.models.dto.PortfolioResponse
import java.math.BigDecimal
import java.util.UUID

object PortfolioService {

    suspend fun getPortfolio(userId: UUID): PortfolioResponse {
        val portfolio = PortfolioRepository.findByUserId(userId)
            ?: throw NotFoundException("Portfolio not found")
        val holdings = PortfolioRepository.findAssets(portfolio.id)
        val assetsById = AssetRepository.findAll().associateBy { it.id }

        val items = holdings.map { pa ->
            val asset = assetsById[pa.assetId]
                ?: throw NotFoundException("Asset ${pa.assetId} missing")
            val currentPrice = asset.lastPrice
            val currentValue = (currentPrice ?: BigDecimal.ZERO).multiply(pa.quantity)
            val invested = pa.avgBuyPrice.multiply(pa.quantity)
            PortfolioAssetResponse(
                assetId = asset.id,
                ticker = asset.ticker,
                name = asset.name,
                quantity = pa.quantity,
                avgBuyPrice = pa.avgBuyPrice,
                currentPrice = currentPrice,
                currentValue = currentValue,
                unrealizedPnl = currentValue.subtract(invested)
            )
        }

        val assetsValue = items.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.currentValue) }
        return PortfolioResponse(
            balanceFictif = portfolio.balanceFictif,
            assetsValue = assetsValue,
            totalValue = portfolio.balanceFictif.add(assetsValue),
            assets = items
        )
    }
}
