package com.kotlinbank.services.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object PriceRefreshJob {

    private val log = LoggerFactory.getLogger(PriceRefreshJob::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    fun start(intervalMillis: Long = 30_000) {
        if (job?.isActive == true) {
            log.warn("Price refresh job already running, ignoring start()")
            return
        }
        log.info("Starting price refresh job (interval=${intervalMillis}ms)")
        job = scope.launch {
            while (isActive) {
                runCatching { MarketDataService.refreshPrices() }
                    .onFailure { log.warn("Price refresh failed: ${it.message}") }
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        log.info("Stopping price refresh job")
        scope.cancel()
    }
}
