package com.kotlinbank.config

object AppConfig {

    val environment: String = env("ENVIRONMENT", "development")
    val port: Int = env("PORT", "8080").toInt()

    object Database {
        val url: String = env("DATABASE_URL", "jdbc:postgresql://localhost:5434/finsim")
        val user: String = env("DATABASE_USER", "finsim")
        val password: String = env("DATABASE_PASSWORD", "finsim_dev")
        val maxPoolSize: Int = env("DATABASE_MAX_POOL", "10").toInt()
    }

    object Redis {
        val url: String = env("REDIS_URL", "redis://localhost:6380")
    }

    object Jwt {
        val secret: String = env("JWT_SECRET", "dev-secret-do-not-use-in-production")
        val issuer: String = env("JWT_ISSUER", "finsim-api")
        val audience: String = env("JWT_AUDIENCE", "finsim-clients")
        val realm: String = env("JWT_REALM", "finsim")
        val expirationMinutes: Long = env("JWT_EXPIRATION_MINUTES", "60").toLong()
    }

    object Market {
        val coinGeckoApiKey: String? = System.getenv("COINGECKO_API_KEY")
    }

    private fun env(key: String, default: String): String =
        System.getProperty(key) ?: System.getenv(key) ?: default
}
