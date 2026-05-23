package com.kotlinbank.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import javax.sql.DataSource

object DatabaseFactory {

    private lateinit var dataSource: HikariDataSource

    fun init() {
        dataSource = HikariDataSource(buildHikariConfig())
        runMigrations(dataSource)
        Database.connect(dataSource)
    }

    fun close() {
        if (::dataSource.isInitialized) dataSource.close()
    }

    private fun buildHikariConfig(): HikariConfig = HikariConfig().apply {
        driverClassName = "org.postgresql.Driver"
        jdbcUrl = AppConfig.Database.url
        username = AppConfig.Database.user
        password = AppConfig.Database.password
        maximumPoolSize = AppConfig.Database.maxPoolSize
        isAutoCommit = false
        transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        validate()
    }

    private fun runMigrations(ds: DataSource) {
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }
}
