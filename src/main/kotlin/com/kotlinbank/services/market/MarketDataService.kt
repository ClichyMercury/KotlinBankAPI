package com.kotlinbank.services.market

import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.models.Asset
import com.kotlinbank.models.AssetType
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object MarketDataService {

    private val log = LoggerFactory.getLogger(MarketDataService::class.java)

    suspend fun listAssets(type: AssetType? = null): List<Asset> =
        AssetRepository.findAll(type)

    suspend fun getAsset(id: UUID): Asset? = AssetRepository.findById(id)

    suspend fun refreshPrices() {
        val cryptos = AssetRepository.findAll(AssetType.CRYPTO)
            .filter { !it.externalId.isNullOrBlank() }
        if (cryptos.isEmpty()) {
            log.debug("No crypto assets to refresh")
            return
        }

        val externalIds = cryptos.mapNotNull { it.externalId }
        val markets = CoinGeckoClient.fetchMarkets(externalIds)
        val byExternalId = markets.associateBy { it.id }

        val now = Instant.now()
        var updated = 0
        cryptos.forEach { asset ->
            val market = byExternalId[asset.externalId] ?: return@forEach
            val price = market.currentPrice ?: return@forEach
            AssetRepository.updateLastPrice(asset.id, BigDecimal.valueOf(price), now)
            updated++
        }
        log.info("Refreshed $updated/${cryptos.size} crypto prices from CoinGecko")
    }
}
