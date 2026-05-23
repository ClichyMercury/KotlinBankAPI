package com.kotlinbank.services

import com.kotlinbank.db.repositories.LedgerRepository
import com.kotlinbank.db.repositories.PortfolioRepository
import com.kotlinbank.db.repositories.UserRepository
import com.kotlinbank.models.LedgerType
import com.kotlinbank.models.dto.AuthResponse
import com.kotlinbank.models.dto.LoginRequest
import com.kotlinbank.models.dto.RegisterRequest
import com.kotlinbank.models.dto.UserResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

object AuthService {

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val pseudoRegex = Regex("^[a-zA-Z0-9_]{3,50}$")

    suspend fun register(req: RegisterRequest): AuthResponse {
        validateRegister(req)
        val email = req.email.trim().lowercase()

        return newSuspendedTransaction(Dispatchers.IO) {
            if (UserRepository.findByEmail(email) != null) {
                throw ConflictException("Email already registered")
            }
            if (UserRepository.findByPseudo(req.pseudo) != null) {
                throw ConflictException("Pseudo already taken")
            }

            val passwordHash = PasswordHasher.hash(req.password)
            val user = UserRepository.create(email, req.pseudo, passwordHash)
            val portfolio = PortfolioRepository.create(user.id)
            LedgerRepository.create(
                userId = user.id,
                type = LedgerType.DEPOSIT,
                amount = portfolio.balanceFictif,
                balanceAfter = portfolio.balanceFictif
            )

            val token = JwtService.issue(user.id, user.email)
            AuthResponse(
                accessToken = token.token,
                expiresInSeconds = token.expiresInSeconds,
                user = UserResponse(user.id, user.email, user.pseudo, user.createdAt)
            )
        }
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        if (req.email.isBlank() || req.password.isBlank()) {
            throw ValidationException("Email and password are required")
        }
        val email = req.email.trim().lowercase()
        val user = UserRepository.findByEmail(email)
            ?: throw UnauthorizedException("Invalid credentials")
        if (!user.isActive) throw UnauthorizedException("Account disabled")
        if (!PasswordHasher.verify(req.password, user.passwordHash)) {
            throw UnauthorizedException("Invalid credentials")
        }

        val token = JwtService.issue(user.id, user.email)
        return AuthResponse(
            accessToken = token.token,
            expiresInSeconds = token.expiresInSeconds,
            user = UserResponse(user.id, user.email, user.pseudo, user.createdAt)
        )
    }

    suspend fun me(userId: UUID): UserResponse {
        val user = UserRepository.findById(userId)
            ?: throw NotFoundException("User not found")
        return UserResponse(user.id, user.email, user.pseudo, user.createdAt)
    }

    private fun validateRegister(req: RegisterRequest) {
        if (!emailRegex.matches(req.email)) {
            throw ValidationException("Invalid email format")
        }
        if (!pseudoRegex.matches(req.pseudo)) {
            throw ValidationException("Pseudo must be 3-50 chars, alphanumeric or underscore")
        }
        if (req.password.length < 8) {
            throw ValidationException("Password must be at least 8 characters")
        }
    }
}
