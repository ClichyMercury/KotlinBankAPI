package com.kotlinbank.config

object AppConfig {

    const val DEV_JWT_SECRET = "dev-secret-do-not-use-in-production"
    const val DEV_DB_PASSWORD = "finsim_dev"
    private const val MIN_SECRET_LENGTH = 32

    val environment: String = env("ENVIRONMENT", "development")
    val port: Int = env("PORT", "8080").toInt()

    object Database {
        val url: String = env("DATABASE_URL", "jdbc:postgresql://localhost:5434/finsim")
        val user: String = env("DATABASE_USER", "finsim")
        val password: String = env("DATABASE_PASSWORD", DEV_DB_PASSWORD)
        val maxPoolSize: Int = env("DATABASE_MAX_POOL", "10").toInt()
    }

    object Redis {
        val url: String = env("REDIS_URL", "redis://localhost:6380")
    }

    object Jwt {
        val secret: String = env("JWT_SECRET", DEV_JWT_SECRET)
        val issuer: String = env("JWT_ISSUER", "finsim-api")
        val audience: String = env("JWT_AUDIENCE", "finsim-clients")
        val realm: String = env("JWT_REALM", "finsim")
        val expirationMinutes: Long = env("JWT_EXPIRATION_MINUTES", "60").toLong()
        val refreshExpirationDays: Long = env("JWT_REFRESH_EXPIRATION_DAYS", "30").toLong()
    }

    object PasswordReset {
        val expirationMinutes: Long = env("PASSWORD_RESET_EXPIRATION_MINUTES", "30").toLong()
    }

    object Mail {
        val provider: String = env("MAIL_PROVIDER", "log")
        val resendApiKey: String = env("RESEND_API_KEY", "")
        val from: String = env("MAIL_FROM", "FinSim <onboarding@resend.dev>")
        val resetUrlTemplate: String = env("PASSWORD_RESET_URL", "https://finsim.app/reset-password?token={token}")
    }

    object Market {
        val coinGeckoApiKey: String? = System.getenv("COINGECKO_API_KEY")
    }

    val isProduction: Boolean get() = environment == "production"

    fun validate(
        environment: String = AppConfig.environment,
        jwtSecret: String = Jwt.secret,
        dbPassword: String = Database.password,
        dbUrl: String = Database.url,
        mailProvider: String = Mail.provider,
        resendApiKey: String = Mail.resendApiKey,
        mailFrom: String = Mail.from,
        resetUrlTemplate: String = Mail.resetUrlTemplate
    ) {
        if (environment != "production") return

        val problems = buildList {
            if (jwtSecret == DEV_JWT_SECRET) {
                add("JWT_SECRET is unset: the API would run with the development secret published in the repository")
            } else if (jwtSecret.length < MIN_SECRET_LENGTH) {
                add("JWT_SECRET is too short (${jwtSecret.length} chars, minimum $MIN_SECRET_LENGTH)")
            }
            if (dbPassword == DEV_DB_PASSWORD) {
                add("DATABASE_PASSWORD is unset: the API would use the development password")
            }
            if (!dbUrl.startsWith("jdbc:postgresql://")) {
                add("DATABASE_URL must be a JDBC url (jdbc:postgresql://host:port/db), got \"${dbUrl.take(24)}...\"")
            }
            if (mailProvider == "log") {
                add("MAIL_PROVIDER is \"log\": password reset emails would never be delivered, set it to \"resend\"")
            } else if (mailProvider != "resend") {
                add("MAIL_PROVIDER must be \"log\" or \"resend\", got \"$mailProvider\"")
            } else {
                if (resendApiKey.isBlank()) add("RESEND_API_KEY is required when MAIL_PROVIDER=resend")
                if (mailFrom.contains("resend.dev")) {
                    add("MAIL_FROM still uses the resend.dev sandbox domain, which only delivers to your own Resend account")
                }
            }
            if (!resetUrlTemplate.contains("{token}")) {
                add("PASSWORD_RESET_URL must contain the {token} placeholder")
            }
        }

        if (problems.isNotEmpty()) {
            throw IllegalStateException(
                problems.joinToString(
                    separator = "\n  - ",
                    prefix = "Refusing to start in production, fix the environment:\n  - "
                )
            )
        }
    }

    private fun env(key: String, default: String): String =
        System.getProperty(key) ?: System.getenv(key) ?: default
}
