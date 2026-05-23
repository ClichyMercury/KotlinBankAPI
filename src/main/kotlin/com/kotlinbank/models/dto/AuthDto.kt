package com.kotlinbank.models.dto

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class RegisterRequest(
    val email: String,
    val pseudo: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    val email: String,
    val pseudo: String,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
