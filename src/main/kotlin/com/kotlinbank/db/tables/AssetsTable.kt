package com.kotlinbank.db.tables

import com.kotlinbank.models.AssetType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object AssetsTable : UUIDTable("assets") {
    val ticker = varchar("ticker", 50).uniqueIndex()
    val name = varchar("name", 255)
    val type = enumerationByName("type", 20, AssetType::class)
    val market = varchar("market", 50)
    val externalId = varchar("external_id", 100).nullable()
    val lastPrice = decimal("last_price", 20, 8).nullable()
    val lastPriceUpdatedAt = timestamp("last_price_updated_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}
