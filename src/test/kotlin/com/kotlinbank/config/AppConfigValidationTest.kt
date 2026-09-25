package com.kotlinbank.config

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AppConfigValidationTest {

    private val strongSecret = "x".repeat(48)
    private val jdbcUrl = "jdbc:postgresql://db.internal:5432/finsim"
    private val resetUrl = "https://finsim.app/reset-password?token={token}"

    private fun validateProduction(
        jwtSecret: String = strongSecret,
        dbPassword: String = "real-password",
        dbUrl: String = jdbcUrl,
        mailProvider: String = "resend",
        resendApiKey: String = "re_live_key",
        mailFrom: String = "FinSim <no-reply@finsim.app>",
        resetUrlTemplate: String = resetUrl,
        corsAllowedHosts: List<String> = listOf("finsim.wharpe.com")
    ) = AppConfig.validate(
        environment = "production",
        jwtSecret = jwtSecret,
        dbPassword = dbPassword,
        dbUrl = dbUrl,
        mailProvider = mailProvider,
        resendApiKey = resendApiKey,
        mailFrom = mailFrom,
        resetUrlTemplate = resetUrlTemplate,
        corsAllowedHosts = corsAllowedHosts
    )

    @Test
    fun `development boots with the dev defaults`() {
        assertDoesNotThrow {
            AppConfig.validate(
                environment = "development",
                jwtSecret = AppConfig.DEV_JWT_SECRET,
                dbPassword = AppConfig.DEV_DB_PASSWORD,
                dbUrl = jdbcUrl,
                mailProvider = "log",
                resendApiKey = "",
                mailFrom = "FinSim <onboarding@resend.dev>",
                resetUrlTemplate = resetUrl,
                corsAllowedHosts = emptyList()
            )
        }
    }

    @Test
    fun `production boots with a proper environment`() {
        assertDoesNotThrow { validateProduction() }
    }

    @Test
    fun `production refuses the dev jwt secret`() {
        val error = assertThrows<IllegalStateException> { validateProduction(jwtSecret = AppConfig.DEV_JWT_SECRET) }
        assertTrue(error.message!!.contains("JWT_SECRET is unset"))
    }

    @Test
    fun `production refuses a short jwt secret`() {
        val error = assertThrows<IllegalStateException> { validateProduction(jwtSecret = "too-short") }
        assertTrue(error.message!!.contains("JWT_SECRET is too short"))
    }

    @Test
    fun `production refuses the dev database password`() {
        val error = assertThrows<IllegalStateException> { validateProduction(dbPassword = AppConfig.DEV_DB_PASSWORD) }
        assertTrue(error.message!!.contains("DATABASE_PASSWORD is unset"))
    }

    @Test
    fun `production refuses a non jdbc database url`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(dbUrl = "postgres://user:pass@host/finsim")
        }
        assertTrue(error.message!!.contains("must be a JDBC url"))
    }

    @Test
    fun `production refuses the log mail provider`() {
        val error = assertThrows<IllegalStateException> { validateProduction(mailProvider = "log") }
        assertTrue(error.message!!.contains("password reset emails would never be delivered"))
    }

    @Test
    fun `production refuses an unknown mail provider`() {
        val error = assertThrows<IllegalStateException> { validateProduction(mailProvider = "sendgrid") }
        assertTrue(error.message!!.contains("MAIL_PROVIDER must be"))
    }

    @Test
    fun `production refuses resend without an api key`() {
        val error = assertThrows<IllegalStateException> { validateProduction(resendApiKey = "") }
        assertTrue(error.message!!.contains("RESEND_API_KEY is required"))
    }

    @Test
    fun `production refuses the resend sandbox sender`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(mailFrom = "FinSim <onboarding@resend.dev>")
        }
        assertTrue(error.message!!.contains("sandbox domain"))
    }

    @Test
    fun `production refuses a reset url without the token placeholder`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(resetUrlTemplate = "https://finsim.app/reset-password")
        }
        assertTrue(error.message!!.contains("{token} placeholder"))
    }

    @Test
    fun `production accepts an empty cors list for a mobile only client`() {
        assertDoesNotThrow { validateProduction(corsAllowedHosts = emptyList()) }
    }

    @Test
    fun `production refuses a cors host carrying a scheme`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(corsAllowedHosts = listOf("https://finsim.wharpe.com"))
        }
        assertTrue(error.message!!.contains("without a scheme"))
        assertTrue(error.message!!.contains("finsim.wharpe.com"))
    }

    @Test
    fun `production refuses a cors host with a trailing slash`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(corsAllowedHosts = listOf("finsim.wharpe.com/"))
        }
        assertTrue(error.message!!.contains("trailing slash"))
    }

    @Test
    fun `cors hosts are parsed from a comma separated list`() {
        assertEquals(
            listOf("finsim.wharpe.com", "api-finsim.wharpe.com"),
            AppConfig.parseHosts(" finsim.wharpe.com , api-finsim.wharpe.com ,, ")
        )
        assertTrue(AppConfig.parseHosts("").isEmpty())
    }

    @Test
    fun `the host serving the reset page is derived from its url`() {
        assertEquals("api-finsim.wharpe.com", AppConfig.hostOf("https://api-finsim.wharpe.com/reset-password?token={token}"))
        assertEquals(null, AppConfig.hostOf("finsim://reset?token={token}"))
        assertEquals(null, AppConfig.hostOf("http://insecure.example/reset"))
        assertEquals(null, AppConfig.hostOf("pas une url"))
    }

    @Test
    fun `production reports every problem at once`() {
        val error = assertThrows<IllegalStateException> {
            validateProduction(
                jwtSecret = AppConfig.DEV_JWT_SECRET,
                dbPassword = AppConfig.DEV_DB_PASSWORD,
                dbUrl = "postgres://host/db",
                mailProvider = "log"
            )
        }
        assertTrue(error.message!!.contains("JWT_SECRET"))
        assertTrue(error.message!!.contains("DATABASE_PASSWORD"))
        assertTrue(error.message!!.contains("DATABASE_URL"))
        assertTrue(error.message!!.contains("MAIL_PROVIDER"))
    }
}
