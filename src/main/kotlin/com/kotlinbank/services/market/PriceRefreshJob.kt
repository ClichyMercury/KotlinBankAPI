package com.kotlinbank.services.market

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.kotlinbank.services.monitoring.SentryReporter
import io.sentry.SentryLevel
import org.slf4j.LoggerFactory

object PriceRefreshJob {

    private const val FAILURES_BEFORE_ALERT = 3

    private val log = LoggerFactory.getLogger(PriceRefreshJob::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var consecutiveFailures = 0

    fun start(intervalMillis: Long = 30_000) {
        if (job?.isActive == true) {
            log.warn("Price refresh job already running, ignoring start()")
            return
        }
        log.info("Starting price refresh job (interval=${intervalMillis}ms)")
        job = scope.launch {
            while (isActive) {
                runCatching { MarketDataService.refreshPrices() }
                    .onSuccess { consecutiveFailures = 0 }
                    .onFailure {
                        consecutiveFailures++
                        log.warn("Price refresh failed ($consecutiveFailures in a row): ${it.message}")
                        if (consecutiveFailures == FAILURES_BEFORE_ALERT) {
                            SentryReporter.captureMessage(
                                "Price refresh failed $FAILURES_BEFORE_ALERT times in a row, prices are going stale",
                                SentryLevel.ERROR,
                                mapOf("job" to "price_refresh")
                            )
                        }
                    }
                delay(intervalMillis)
            }
        }
    }

    fun stop() {
        log.info("Stopping price refresh job")
        scope.cancel()
    }
}
