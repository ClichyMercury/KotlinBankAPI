package com.kotlinbank.config

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands

object RedisFactory {

    private lateinit var client: RedisClient
    private lateinit var connection: StatefulRedisConnection<String, String>

    fun init() {
        val uri = RedisURI.create(AppConfig.Redis.url)
        client = RedisClient.create(uri)
        connection = client.connect()
        connection.sync().ping()
    }

    fun sync(): RedisCommands<String, String> = connection.sync()

    fun close() {
        if (::connection.isInitialized) connection.close()
        if (::client.isInitialized) client.shutdown()
    }
}
