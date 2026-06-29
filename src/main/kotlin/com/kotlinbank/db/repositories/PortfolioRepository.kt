package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.PortfolioAssetsTable
import com.kotlinbank.db.tables.PortfoliosTable
import com.kotlinbank.models.Portfolio
import com.kotlinbank.models.PortfolioAsset
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object PortfolioRepository {

    suspend fun findByUserId(userId: UUID): Portfolio? = newSuspendedTransaction(Dispatchers.IO) {
        PortfoliosTable.select { PortfoliosTable.userId eq userId }
            .singleOrNull()?.toPortfolio()
    }

    suspend fun create(userId: UUID): Portfolio = newSuspendedTransaction(Dispatchers.IO) {
        val id = UUID.randomUUID()
        PortfoliosTable.insert {
            it[PortfoliosTable.id] = id
            it[PortfoliosTable.userId] = userId
        }
        PortfoliosTable.select { PortfoliosTable.id eq id }.single().toPortfolio()
    }

    suspend fun updateBalance(portfolioId: UUID, newBalance: BigDecimal): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            PortfoliosTable.update({ PortfoliosTable.id eq portfolioId }) {
                it[balanceFictif] = newBalance
            }
        }

    suspend fun findAssets(portfolioId: UUID): List<PortfolioAsset> =
        newSuspendedTransaction(Dispatchers.IO) {
            PortfolioAssetsTable
                .select { PortfolioAssetsTable.portfolioId eq portfolioId }
                .map { it.toPortfolioAsset() }
        }

    suspend fun findAssetByPair(portfolioId: UUID, assetId: UUID): PortfolioAsset? =
        newSuspendedTransaction(Dispatchers.IO) {
            PortfolioAssetsTable.select {
                (PortfolioAssetsTable.portfolioId eq portfolioId) and
                    (PortfolioAssetsTable.assetId eq assetId)
            }.singleOrNull()?.toPortfolioAsset()
        }

    suspend fun addAsset(
        portfolioId: UUID,
        assetId: UUID,
        quantity: BigDecimal,
        avgBuyPrice: BigDecimal
    ): PortfolioAsset = newSuspendedTransaction(Dispatchers.IO) {
        val id = UUID.randomUUID()
        PortfolioAssetsTable.insert {
            it[PortfolioAssetsTable.id] = id
            it[PortfolioAssetsTable.portfolioId] = portfolioId
            it[PortfolioAssetsTable.assetId] = assetId
            it[PortfolioAssetsTable.quantity] = quantity
            it[PortfolioAssetsTable.avgBuyPrice] = avgBuyPrice
        }
        PortfolioAssetsTable.select { PortfolioAssetsTable.id eq id }.single().toPortfolioAsset()
    }

    suspend fun updateAsset(
        id: UUID,
        quantity: BigDecimal,
        avgBuyPrice: BigDecimal
    ): Int = newSuspendedTransaction(Dispatchers.IO) {
        PortfolioAssetsTable.update({ PortfolioAssetsTable.id eq id }) {
            it[PortfolioAssetsTable.quantity] = quantity
            it[PortfolioAssetsTable.avgBuyPrice] = avgBuyPrice
            it[PortfolioAssetsTable.updatedAt] = Instant.now()
        }
    }

    suspend fun removeAsset(id: UUID): Int = newSuspendedTransaction(Dispatchers.IO) {
        PortfolioAssetsTable.deleteWhere { Op.build { PortfolioAssetsTable.id eq id } }
    }

    private fun ResultRow.toPortfolio(): Portfolio = Portfolio(
        id = this[PortfoliosTable.id].value,
        userId = this[PortfoliosTable.userId].value,
        balanceFictif = this[PortfoliosTable.balanceFictif],
        createdAt = this[PortfoliosTable.createdAt]
    )

    private fun ResultRow.toPortfolioAsset(): PortfolioAsset = PortfolioAsset(
        id = this[PortfolioAssetsTable.id].value,
        portfolioId = this[PortfolioAssetsTable.portfolioId].value,
        assetId = this[PortfolioAssetsTable.assetId].value,
        quantity = this[PortfolioAssetsTable.quantity],
        avgBuyPrice = this[PortfolioAssetsTable.avgBuyPrice],
        updatedAt = this[PortfolioAssetsTable.updatedAt]
    )
}
