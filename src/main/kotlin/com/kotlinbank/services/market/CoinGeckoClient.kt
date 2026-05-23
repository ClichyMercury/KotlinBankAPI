package com.kotlinbank.services.market

import com.kotlinbank.config.AppConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object CoinGeckoClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
        install(UserAgent) {
            agent = "FinSim/0.1 (gael.sassan@softskiils.ci)"
        }
        defaultRequest {
            url("https://api.coingecko.com/api/v3/")
            AppConfig.Market.coinGeckoApiKey?.let { header("x-cg-demo-api-key", it) }
        }
    }

    suspend fun fetchMarkets(coinIds: List<String>): List<CoinGeckoMarket> {
        if (coinIds.isEmpty()) return emptyList()
        return client.get("coins/markets") {
            parameter("vs_currency", "usd")
            parameter("ids", coinIds.joinToString(","))
            parameter("per_page", 250)
            parameter("page", 1)
            parameter("sparkline", "false")
        }.body()
    }

    fun close() = client.close()
}

@Serializable
data class CoinGeckoMarket(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price") val currentPrice: Double? = null
)
