package com.kotlinbank.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val db: Boolean,
    val redis: Boolean
)
