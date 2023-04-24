package com.example.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

data class ConnectionPool(
    val jdbcUrl: String,
    val username: String = "",
    val password: String = "",
    val poolSize: Int = 10,
) {
    private val ds: HikariDataSource

    init {
        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl
        config.username = username
        config.password = password
        config.maximumPoolSize = poolSize
        ds = HikariDataSource(config)
    }

    fun connection(): Connection = ds.connection
}
