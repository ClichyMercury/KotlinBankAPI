package com.kotlinbank

import com.kotlinbank.config.DatabaseFactory
import com.kotlinbank.config.RedisFactory
import com.kotlinbank.db.AssetSeeder
import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.models.OrderStatus
import com.kotlinbank.models.dto.AssetResponse
import com.kotlinbank.models.dto.AuthResponse
import com.kotlinbank.models.dto.BuyOrderRequest
import com.kotlinbank.models.dto.SellOrderRequest
import com.kotlinbank.models.dto.ErrorResponse
import com.kotlinbank.models.dto.LoginRequest
import com.kotlinbank.models.dto.OrderResponse
import com.kotlinbank.models.dto.PortfolioResponse
import com.kotlinbank.models.dto.RegisterRequest
import com.kotlinbank.models.dto.UserResponse
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Requires dev docker compose stack running on ports 5434 (postgres) / 6380 (redis).
 * Must NOT run concurrently with `./gradlew run` — the price refresh job would race.
 */
class EndToEndFlowTest {

    companion object {

        @BeforeAll
        @JvmStatic
        fun setup() {
            System.setProperty("DATABASE_URL", "jdbc:postgresql://localhost:5434/finsim")
            System.setProperty("DATABASE_USER", "finsim")
            System.setProperty("DATABASE_PASSWORD", "finsim_dev")
            System.setProperty("REDIS_URL", "redis://localhost:6380")
            System.setProperty("JWT_SECRET", "test-secret-very-long-string-for-testing-only")

            DatabaseFactory.init()
            RedisFactory.init()
            runBlocking {
                AssetSeeder.seedIfEmpty()
                val btc = AssetRepository.findByTicker("BTC")
                    ?: error("BTC asset missing after seed")
                AssetRepository.updateLastPrice(btc.id, BigDecimal("70000"), Instant.now())
            }
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            RedisFactory.close()
            DatabaseFactory.close()
        }
    }

    @Test
    fun `full E2E flow register login portfolio buy orders`() = testApplication {
        application { module() }
        val http = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

        val stamp = System.currentTimeMillis()
        val email = "e2e-$stamp@test.io"
        val pseudo = "e2e_$stamp"

        val regResp = http.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(email, pseudo, "password123"))
        }
        assertEquals(HttpStatusCode.Created, regResp.status, "register should return 201")
        val regBody = regResp.body<AuthResponse>()
        val token = regBody.accessToken
        assertTrue(token.isNotBlank(), "access token must be present")

        val loginResp = http.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, "password123"))
        }
        assertEquals(HttpStatusCode.OK, loginResp.status)
        assertEquals(regBody.user.id, loginResp.body<AuthResponse>().user.id)

        val meResp = http.get("/api/v1/auth/me") { header("Authorization", "Bearer $token") }
        assertEquals(HttpStatusCode.OK, meResp.status)
        assertEquals(email, meResp.body<UserResponse>().email)

        val portfolio1 = http.get("/api/v1/portfolio") {
            header("Authorization", "Bearer $token")
        }.body<PortfolioResponse>()
        assertEquals(0, portfolio1.balanceFictif.compareTo(BigDecimal("10000")), "initial balance is 10000")
        assertTrue(portfolio1.assets.isEmpty(), "portfolio starts empty")

        val assets = http.get("/api/v1/market/assets").body<List<AssetResponse>>()
        val btc = assets.first { it.ticker == "BTC" }
        assertNotNull(btc.lastPrice, "BTC must have a price")
        assertEquals(0, btc.lastPrice!!.compareTo(BigDecimal("70000")), "BTC price was pinned to 70000")

        val buyResp = http.post("/api/v1/orders/buy") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(BuyOrderRequest(btc.id, BigDecimal("0.1")))
        }
        assertEquals(HttpStatusCode.Created, buyResp.status)
        val order = buyResp.body<OrderResponse>()
        assertEquals(OrderStatus.EXECUTED, order.status)
        assertEquals(0, order.total.compareTo(BigDecimal("7000")), "0.1 BTC × 70000 = 7000")

        val portfolio2 = http.get("/api/v1/portfolio") {
            header("Authorization", "Bearer $token")
        }.body<PortfolioResponse>()
        assertEquals(0, portfolio2.balanceFictif.compareTo(BigDecimal("3000")), "balance after buy")
        assertEquals(1, portfolio2.assets.size, "portfolio holds 1 asset")
        assertEquals("BTC", portfolio2.assets[0].ticker)
        assertEquals(0, portfolio2.assets[0].quantity.compareTo(BigDecimal("0.1")))

        val orders = http.get("/api/v1/orders") {
            header("Authorization", "Bearer $token")
        }.body<List<OrderResponse>>()
        assertTrue(orders.isNotEmpty(), "order history non-empty")
        assertEquals(order.id, orders[0].id, "most recent order matches")

        // --- SELL flow ---
        // Re-pin BTC higher so the realized PnL is non-zero and deterministic.
        runBlocking {
            val btcAsset = AssetRepository.findByTicker("BTC") ?: error("BTC missing")
            AssetRepository.updateLastPrice(btcAsset.id, BigDecimal("80000"), Instant.now())
        }

        val sellResp = http.post("/api/v1/orders/sell") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SellOrderRequest(btc.id, BigDecimal("0.05")))
        }
        assertEquals(HttpStatusCode.Created, sellResp.status)
        val sellOrder = sellResp.body<OrderResponse>()
        assertEquals(OrderStatus.EXECUTED, sellOrder.status)
        assertEquals(0, sellOrder.total.compareTo(BigDecimal("4000")), "0.05 BTC × 80000 = 4000")
        assertNotNull(sellOrder.realizedPnl, "sell exposes realized PnL")
        assertEquals(0, sellOrder.realizedPnl!!.compareTo(BigDecimal("500")), "(80000-70000)×0.05 = 500")

        val portfolio3 = http.get("/api/v1/portfolio") {
            header("Authorization", "Bearer $token")
        }.body<PortfolioResponse>()
        assertEquals(0, portfolio3.balanceFictif.compareTo(BigDecimal("7000")), "3000 + 4000 proceeds")
        assertEquals(0, portfolio3.assets[0].quantity.compareTo(BigDecimal("0.05")), "0.1 - 0.05 left")

        // Oversell is rejected
        val badSell = http.post("/api/v1/orders/sell") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SellOrderRequest(btc.id, BigDecimal("100")))
        }
        assertEquals(HttpStatusCode.BadRequest, badSell.status)
        assertTrue(badSell.body<ErrorResponse>().message.contains("Insufficient quantity"))

        // Selling the rest removes the position entirely
        val sellAll = http.post("/api/v1/orders/sell") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SellOrderRequest(btc.id, BigDecimal("0.05")))
        }
        assertEquals(HttpStatusCode.Created, sellAll.status)
        val portfolio4 = http.get("/api/v1/portfolio") {
            header("Authorization", "Bearer $token")
        }.body<PortfolioResponse>()
        assertEquals(0, portfolio4.balanceFictif.compareTo(BigDecimal("11000")), "7000 + 4000 proceeds")
        assertTrue(portfolio4.assets.isEmpty(), "position fully closed -> removed")

        val badBuy = http.post("/api/v1/orders/buy") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(BuyOrderRequest(btc.id, BigDecimal("100")))
        }
        assertEquals(HttpStatusCode.BadRequest, badBuy.status)
        val err = badBuy.body<ErrorResponse>()
        assertEquals("validation_error", err.error)
        assertTrue(err.message.contains("Insufficient"), "should mention insufficient balance")

        val noToken = http.get("/api/v1/portfolio")
        assertEquals(HttpStatusCode.Unauthorized, noToken.status)
    }
}
