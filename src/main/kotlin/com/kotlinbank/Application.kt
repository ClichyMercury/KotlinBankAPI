package com.kotlinbank

import com.kotlinbank.config.AppConfig
import com.kotlinbank.config.DatabaseFactory
import com.kotlinbank.config.RedisFactory
import com.kotlinbank.db.AssetSeeder
import com.kotlinbank.db.repositories.PasswordResetTokenRepository
import com.kotlinbank.db.repositories.RefreshTokenRepository
import com.kotlinbank.plugins.configureAuth
import com.kotlinbank.plugins.configureCORS
import com.kotlinbank.plugins.configureCallLogging
import com.kotlinbank.plugins.configureRateLimit
import com.kotlinbank.plugins.configureRouting
import com.kotlinbank.plugins.configureSerialization
import com.kotlinbank.plugins.configureStatusPages
import com.kotlinbank.services.mail.MailService
import com.kotlinbank.services.market.CoinGeckoClient
import com.kotlinbank.services.market.PriceRefreshJob
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("Application")

fun main() {
    log.info("Starting FinSim API on port ${AppConfig.port} (env=${AppConfig.environment})")

    runCatching { AppConfig.validate() }.onFailure {
        log.error(it.message)
        exitProcess(1)
    }

    DatabaseFactory.init()
    RedisFactory.init()
    runBlocking {
        AssetSeeder.seedIfEmpty()
        val purged = RefreshTokenRepository.deleteExpired()
        if (purged > 0) log.info("Purged $purged expired refresh tokens")
        val purgedResets = PasswordResetTokenRepository.deleteExpired()
        if (purgedResets > 0) log.info("Purged $purgedResets expired password reset tokens")
    }
    PriceRefreshJob.start()

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown: stopping job, closing DB + Redis + HTTP clients")
        PriceRefreshJob.stop()
        CoinGeckoClient.close()
        MailService.close()
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
