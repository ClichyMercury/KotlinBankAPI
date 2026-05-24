package com.kotlinbank.services.market

import com.kotlinbank.config.RedisFactory
import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.models.Asset
import com.kotlinbank.models.AssetType
import com.kotlinbank.models.dto.CandleResponse
import com.kotlinbank.services.NotFoundException
import com.kotlinbank.services.ValidationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object MarketDataService {

    private val log = LoggerFactory.getLogger(MarketDataService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    private val allowedDays = setOf(1, 7, 30, 365)
    private val cacheTtlSecondsByDays = mapOf(
        1 to 60L,         // intraday : refresh fréquent
        7 to 300L,        // semaine : 5 min
        30 to 1800L,      // mois : 30 min
        365 to 21600L     // année : 6 h
    )

    suspend fun listAssets(type: AssetType? = null): List<Asset> =
        AssetRepository.findAll(type)

    suspend fun getAsset(id: UUID): Asset? = AssetRepository.findById(id)

    suspend fun getCandles(assetId: UUID, days: Int): List<CandleResponse> {
        if (days !in allowedDays) {
            throw ValidationException("Invalid days: must be one of ${allowedDays.sorted()}")
        }
        val asset = AssetRepository.findById(assetId)
            ?: throw NotFoundException("Asset not found")
        val externalId = asset.externalId
            ?: throw ValidationException("Asset ${asset.ticker} has no external_id (candles unsupported)")
        if (asset.type != AssetType.CRYPTO) {
            throw ValidationException("Candles currently supported for CRYPTO only")
        }

        val cacheKey = "candles:$externalId:$days"
        val cached = runCatching { RedisFactory.sync().get(cacheKey) }.getOrNull()
        if (cached != null) {
            return json.decodeFromString(ListSerializer(CandleResponse.serializer()), cached)
        }

        val raw = CoinGeckoClient.fetchOhlc(externalId, days)
        val candles = raw.mapNotNull { row ->
            if (row.size < 5) return@mapNotNull null
            CandleResponse(
                timestamp = Instant.ofEpochMilli(row[0].toLong()),
                open = BigDecimal.valueOf(row[1]),
                high = BigDecimal.valueOf(row[2]),
                low = BigDecimal.valueOf(row[3]),
                close = BigDecimal.valueOf(row[4])
            )
        }

        runCatching {
            val payload = json.encodeToString(ListSerializer(CandleResponse.serializer()), candles)
            val ttl = cacheTtlSecondsByDays[days] ?: 300L
            RedisFactory.sync().setex(cacheKey, ttl, payload)
        }.onFailure { log.warn("Failed to cache candles: ${it.message}") }

        log.info("Fetched ${candles.size} OHLC candles for ${asset.ticker} (days=$days)")
        return candles
    }

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
