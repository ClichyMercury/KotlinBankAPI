package com.kotlinbank.db

import com.kotlinbank.db.repositories.AssetRepository
import com.kotlinbank.db.repositories.AssetRepository.NewAsset
import com.kotlinbank.models.AssetType
import org.slf4j.LoggerFactory

object AssetSeeder {

    private val log = LoggerFactory.getLogger(AssetSeeder::class.java)

    private val cryptos = listOf(
        NewAsset("BTC", "Bitcoin", AssetType.CRYPTO, "global", "bitcoin"),
        NewAsset("ETH", "Ethereum", AssetType.CRYPTO, "global", "ethereum"),
        NewAsset("USDT", "Tether", AssetType.CRYPTO, "global", "tether"),
        NewAsset("SOL", "Solana", AssetType.CRYPTO, "global", "solana"),
        NewAsset("ADA", "Cardano", AssetType.CRYPTO, "global", "cardano")
    )

    suspend fun seedIfEmpty() {
        val count = AssetRepository.countAll()
        if (count == 0L) {
            AssetRepository.batchCreate(cryptos)
            log.info("Seeded ${cryptos.size} crypto assets")
        } else {
            log.info("Skipping seed: $count assets already present")
        }
    }
}
