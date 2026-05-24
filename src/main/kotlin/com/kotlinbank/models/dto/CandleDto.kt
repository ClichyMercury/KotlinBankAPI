package com.kotlinbank.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class CandleResponse(
    @Serializable(with = InstantSerializer::class) val timestamp: Instant,
    @Serializable(with = BigDecimalSerializer::class) val open: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val high: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val low: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class) val close: BigDecimal
)
