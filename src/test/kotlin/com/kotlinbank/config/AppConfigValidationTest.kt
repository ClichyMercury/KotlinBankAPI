package com.kotlinbank.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AppConfigValidationTest {

    private val strongSecret = "x".repeat(48)
    private val jdbcUrl = "jdbc:postgresql://db.internal:5432/finsim"

    @Test
    fun `development boots with the dev defaults`() {
        assertDoesNotThrow {
            AppConfig.validate("development", AppConfig.DEV_JWT_SECRET, AppConfig.DEV_DB_PASSWORD, jdbcUrl)
        }
    }

    @Test
    fun `production refuses the dev jwt secret`() {
        val error = assertThrows<IllegalStateException> {
            AppConfig.validate("production", AppConfig.DEV_JWT_SECRET, "real-password", jdbcUrl)
        }
        assertTrue(error.message!!.contains("JWT_SECRET is unset"))
    }

    @Test
    fun `production refuses a short jwt secret`() {
        val error = assertThrows<IllegalStateException> {
            AppConfig.validate("production", "too-short", "real-password", jdbcUrl)
        }
        assertTrue(error.message!!.contains("JWT_SECRET is too short"))
    }

    @Test
    fun `production refuses the dev database password`() {
        val error = assertThrows<IllegalStateException> {
            AppConfig.validate("production", strongSecret, AppConfig.DEV_DB_PASSWORD, jdbcUrl)
        }
        assertTrue(error.message!!.contains("DATABASE_PASSWORD is unset"))
    }

    @Test
    fun `production refuses a non jdbc database url`() {
        val error = assertThrows<IllegalStateException> {
            AppConfig.validate("production", strongSecret, "real-password", "postgres://user:pass@host/finsim")
        }
        assertTrue(error.message!!.contains("must be a JDBC url"))
    }

    @Test
    fun `production reports every problem at once`() {
        val error = assertThrows<IllegalStateException> {
            AppConfig.validate("production", AppConfig.DEV_JWT_SECRET, AppConfig.DEV_DB_PASSWORD, "postgres://host/db")
        }
        assertTrue(error.message!!.contains("JWT_SECRET"))
        assertTrue(error.message!!.contains("DATABASE_PASSWORD"))
        assertTrue(error.message!!.contains("DATABASE_URL"))
    }

    @Test
    fun `production boots with a proper environment`() {
        assertDoesNotThrow {
            AppConfig.validate("production", strongSecret, "real-password", jdbcUrl)
        }
    }
}
