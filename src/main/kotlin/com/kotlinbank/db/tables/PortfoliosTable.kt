package com.kotlinbank.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.math.BigDecimal
import java.time.Instant

object PortfoliosTable : UUIDTable("portfolios") {
    val userId = reference("user_id", UsersTable).uniqueIndex()
    val balanceFictif = decimal("balance_fictif", 20, 2).default(BigDecimal("10000"))
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
