package com.kotlinbank.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val pseudo = varchar("pseudo", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
