package com.kotlinbank.models

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val pseudo: String,
    val passwordHash: String,
    val isActive: Boolean,
    val createdAt: Instant
)
