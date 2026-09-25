package com.kotlinbank.services.monitoring

import com.kotlinbank.config.AppConfig
import io.sentry.Sentry
import io.sentry.SentryLevel
import org.slf4j.LoggerFactory

object SentryReporter {

    private val log = LoggerFactory.getLogger(SentryReporter::class.java)

    fun init() {
        val dsn = AppConfig.Monitoring.sentryDsn
        if (dsn.isBlank()) {
            if (AppConfig.isProduction) {
                log.warn("SENTRY_DSN is unset: errors will only reach the container logs")
            } else {
                log.info("SENTRY_DSN is unset, error reporting disabled")
            }
            return
        }

        Sentry.init { options ->
            options.dsn = dsn
            options.environment = AppConfig.environment
            options.isSendDefaultPii = false
            options.tracesSampleRate = AppConfig.Monitoring.tracesSampleRate
            AppConfig.Monitoring.release.takeIf { it.isNotBlank() }?.let { options.release = it }
        }
        log.info("Sentry enabled for environment ${AppConfig.environment}")
    }

    fun close() {
        if (Sentry.isEnabled()) Sentry.close()
    }

    fun captureException(throwable: Throwable, context: Map<String, String> = emptyMap()) {
        if (!Sentry.isEnabled()) return
        Sentry.withScope { scope ->
            context.forEach { (key, value) -> scope.setTag(key, value) }
            Sentry.captureException(throwable)
        }
    }

    fun captureMessage(message: String, level: SentryLevel, context: Map<String, String> = emptyMap()) {
        if (!Sentry.isEnabled()) return
        Sentry.withScope { scope ->
            context.forEach { (key, value) -> scope.setTag(key, value) }
            Sentry.captureMessage(message, level)
        }
    }

    fun scrubUrl(url: String): String = url.substringBefore('?').substringBefore('#')

    fun requestContext(method: String, uri: String, userId: String?): Map<String, String> = buildMap {
        put("http.method", method)
        put("http.path", scrubUrl(uri))
        userId?.let { put("user.id", it) }
    }
}
