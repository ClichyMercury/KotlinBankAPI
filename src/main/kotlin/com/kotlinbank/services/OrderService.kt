package com.kotlinbank.services

import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.db.repositories.LedgerRepository
import com.kotlinbank.db.repositories.OrderRepository
import com.kotlinbank.db.repositories.PortfolioRepository
import com.kotlinbank.models.LedgerType
import com.kotlinbank.models.Order
import com.kotlinbank.models.OrderStatus
import com.kotlinbank.models.OrderType
import com.kotlinbank.models.dto.BuyOrderRequest
import com.kotlinbank.models.dto.OrderResponse
import com.kotlinbank.models.dto.SellOrderRequest
import com.kotlinbank.models.dto.toResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

object OrderService {

    private const val PRICE_SCALE = 8
    private const val MONEY_SCALE = 2

    suspend fun placeBuyOrder(userId: UUID, req: BuyOrderRequest): OrderResponse {
        if (req.quantity <= BigDecimal.ZERO) {
            throw ValidationException("Quantity must be strictly positive")
        }

        return newSuspendedTransaction(Dispatchers.IO) {
            val asset = AssetRepository.findById(req.assetId)
                ?: throw NotFoundException("Asset not found")
            val price = asset.lastPrice
                ?: throw ValidationException("Asset ${asset.ticker} has no current price")

            val portfolio = PortfolioRepository.findByUserId(userId)
                ?: throw NotFoundException("Portfolio not found")

            val totalCost = req.quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            if (portfolio.balanceFictif < totalCost) {
                throw ValidationException(
                    "Insufficient balance: need $totalCost, have ${portfolio.balanceFictif}"
                )
            }

            val executedAt = Instant.now()
            val order: Order = OrderRepository.create(
                userId = userId,
                assetId = asset.id,
                type = OrderType.BUY,
                quantity = req.quantity,
                price = price,
                status = OrderStatus.EXECUTED,
                executedAt = executedAt
            )

            val existing = PortfolioRepository.findAssetByPair(portfolio.id, asset.id)
            if (existing == null) {
                PortfolioRepository.addAsset(
                    portfolioId = portfolio.id,
                    assetId = asset.id,
                    quantity = req.quantity,
                    avgBuyPrice = price
                )
            } else {
                val newQty = existing.quantity.add(req.quantity)
                val newAvg = existing.quantity.multiply(existing.avgBuyPrice)
                    .add(req.quantity.multiply(price))
                    .divide(newQty, PRICE_SCALE, RoundingMode.HALF_UP)
                PortfolioRepository.updateAsset(existing.id, newQty, newAvg)
            }

            val newBalance = portfolio.balanceFictif.subtract(totalCost)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            PortfolioRepository.updateBalance(portfolio.id, newBalance)

            LedgerRepository.create(
                userId = userId,
                type = LedgerType.BUY,
                amount = totalCost.negate(),
                balanceAfter = newBalance,
                orderId = order.id
            )

            order.toResponse()
        }
    }

    suspend fun placeSellOrder(userId: UUID, req: SellOrderRequest): OrderResponse {
        if (req.quantity <= BigDecimal.ZERO) {
            throw ValidationException("Quantity must be strictly positive")
        }

        return newSuspendedTransaction(Dispatchers.IO) {
            val asset = AssetRepository.findById(req.assetId)
                ?: throw NotFoundException("Asset not found")
            val price = asset.lastPrice
                ?: throw ValidationException("Asset ${asset.ticker} has no current price")

            val portfolio = PortfolioRepository.findByUserId(userId)
                ?: throw NotFoundException("Portfolio not found")

            val position = PortfolioRepository.findAssetByPair(portfolio.id, asset.id)
                ?: throw ValidationException("No ${asset.ticker} position to sell")
            if (position.quantity < req.quantity) {
                throw ValidationException(
                    "Insufficient quantity: trying to sell ${req.quantity}, hold ${position.quantity}"
                )
            }

            val proceeds = req.quantity.multiply(price).setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            // PnL réalisé = (prix de vente - coût moyen d'achat) × quantité vendue
            val realizedPnl = price.subtract(position.avgBuyPrice)
                .multiply(req.quantity)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)

            val executedAt = Instant.now()
            val order: Order = OrderRepository.create(
                userId = userId,
                assetId = asset.id,
                type = OrderType.SELL,
                quantity = req.quantity,
                price = price,
                status = OrderStatus.EXECUTED,
                executedAt = executedAt
            )

            // Décrémente la position ; supprime la ligne si on solde tout (avg_buy_price inchangé sur un sell)
            val remainingQty = position.quantity.subtract(req.quantity)
            if (remainingQty.compareTo(BigDecimal.ZERO) == 0) {
                PortfolioRepository.removeAsset(position.id)
            } else {
                PortfolioRepository.updateAsset(position.id, remainingQty, position.avgBuyPrice)
            }

            val newBalance = portfolio.balanceFictif.add(proceeds)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            PortfolioRepository.updateBalance(portfolio.id, newBalance)

            LedgerRepository.create(
                userId = userId,
                type = LedgerType.SELL,
                amount = proceeds,
                balanceAfter = newBalance,
                orderId = order.id
            )

            order.toResponse(realizedPnl = realizedPnl)
        }
    }

    suspend fun listUserOrders(userId: UUID, limit: Int = 50): List<OrderResponse> =
        OrderRepository.findByUserId(userId, limit).map { it.toResponse() }
}
