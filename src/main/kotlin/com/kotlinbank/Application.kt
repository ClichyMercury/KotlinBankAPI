package com.kotlinbank

import com.kotlinbank.config.AppConfig
import com.kotlinbank.config.DatabaseFactory
import com.kotlinbank.config.RedisFactory
import com.kotlinbank.db.AssetSeeder
import com.kotlinbank.plugins.configureAuth
import com.kotlinbank.plugins.configureCORS
import com.kotlinbank.plugins.configureCallLogging
import com.kotlinbank.plugins.configureRateLimit
import com.kotlinbank.plugins.configureRouting
import com.kotlinbank.plugins.configureSerialization
import com.kotlinbank.plugins.configureStatusPages
import com.kotlinbank.services.market.CoinGeckoClient
import com.kotlinbank.services.market.PriceRefreshJob
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("Application")

fun main() {
    log.info("Starting FinSim API on port ${AppConfig.port} (env=${AppConfig.environment})")

    DatabaseFactory.init()
    RedisFactory.init()
    runBlocking { AssetSeeder.seedIfEmpty() }
    PriceRefreshJob.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown: stopping job, closing DB + Redis + HTTP client")
        PriceRefreshJob.stop()
        CoinGeckoClient.close()
        RedisFactory.close()
        DatabaseFactory.close()
    })

    embeddedServer(
        factory = Netty,
        port = AppConfig.port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureCallLogging()
    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureRateLimit()
    configureAuth()
    configureRouting()
}
