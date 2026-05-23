package com.kotlinbank.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

val RATE_LIMIT_AUTH = RateLimitName("auth")

fun Application.configureRateLimit() {
    install(RateLimit) {
        register(RATE_LIMIT_AUTH) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }
}
