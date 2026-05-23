package com.kotlinbank.db.repositories

import com.kotlinbank.db.tables.AssetsTable
import com.kotlinbank.models.Asset
import com.kotlinbank.models.AssetType
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object AssetRepository {

    data class NewAsset(
        val ticker: String,
        val name: String,
        val type: AssetType,
        val market: String,
        val externalId: String?
    )

    suspend fun findAll(type: AssetType? = null): List<Asset> =
        newSuspendedTransaction(Dispatchers.IO) {
            val query = if (type != null) {
                AssetsTable.select { AssetsTable.type eq type }
            } else {
                AssetsTable.selectAll()
            }
            query.map { it.toAsset() }
        }

    suspend fun findById(id: UUID): Asset? = newSuspendedTransaction(Dispatchers.IO) {
        AssetsTable.select { AssetsTable.id eq id }.singleOrNull()?.toAsset()
    }

    suspend fun findByTicker(ticker: String): Asset? = newSuspendedTransaction(Dispatchers.IO) {
        AssetsTable.select { AssetsTable.ticker eq ticker }.singleOrNull()?.toAsset()
    }

    suspend fun countAll(): Long = newSuspendedTransaction(Dispatchers.IO) {
        AssetsTable.selectAll().count()
    }

    suspend fun updateLastPrice(id: UUID, price: BigDecimal, updatedAt: Instant): Int =
        newSuspendedTransaction(Dispatchers.IO) {
            AssetsTable.update({ AssetsTable.id eq id }) {
                it[lastPrice] = price
                it[lastPriceUpdatedAt] = updatedAt
            }
        }

    suspend fun batchCreate(assets: List<NewAsset>) = newSuspendedTransaction(Dispatchers.IO) {
        AssetsTable.batchInsert(assets) { a ->
            this[AssetsTable.ticker] = a.ticker
            this[AssetsTable.name] = a.name
            this[AssetsTable.type] = a.type
            this[AssetsTable.market] = a.market
            this[AssetsTable.externalId] = a.externalId
        }
        Unit
    }

    private fun ResultRow.toAsset(): Asset = Asset(
        id = this[AssetsTable.id].value,
        ticker = this[AssetsTable.ticker],
        name = this[AssetsTable.name],
        type = this[AssetsTable.type],
        market = this[AssetsTable.market],
        externalId = this[AssetsTable.externalId],
        lastPrice = this[AssetsTable.lastPrice],
        lastPriceUpdatedAt = this[AssetsTable.lastPriceUpdatedAt],
        createdAt = this[AssetsTable.createdAt]
    )
}
