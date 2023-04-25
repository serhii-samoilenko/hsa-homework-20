package com.example.util

import java.sql.ResultSet.CONCUR_READ_ONLY
import java.sql.ResultSet.TYPE_FORWARD_ONLY

class Database(private val pool: ConnectionPool) {
    fun execute(vararg sql: String, report: Report? = null) = execute(sql.toList(), report)

    fun execute(sql: List<String>, report: Report? = null) {
        report?.sql(sql.toList())
        pool.connection().use { connection ->
            sql.forEach {
                connection.createStatement().executeUpdate(it)
            }
        }
    }

//    fun execute(sql: String, r: Report? = null): Duration = execute(listOf(sql), r)

    fun tryExecute(sql: String, report: Report? = null): String? = try {
        execute(listOf(sql), report)
        null
    } catch (e: Exception) {
        println("Got exception: ${e.message}")
        e.message
    }

    fun queryData(sql: String, report: Report? = null): List<Map<String, Any?>> {
        report?.sql(sql)
        pool.connection().use { connection ->
            connection.createStatement().executeQuery(sql).use { rs ->
                val columns = (1..rs.metaData.columnCount).map { rs.metaData.getColumnName(it) }
                val rows = mutableListOf<Map<String, Any?>>()
                while (rs.next()) {
                    rows.add(columns.associateWith { rs.getObject(it) })
                }
                rs.close()
                return rows
            }
        }
    }

    fun query(sql: String, report: Report? = null) {
        report?.sql(sql)
        pool.connection().use { connection ->
            connection
                .createStatement(TYPE_FORWARD_ONLY, CONCUR_READ_ONLY)
                .apply { fetchSize = 2000 }
                .executeQuery(sql).use { rs ->
                    while (rs.next()) {
                        // do nothing
                    }
                    rs.close()
                }
        }
    }

    fun querySingleValue(sql: String, report: Report? = null): Any = queryData(sql, report).first().values.first()!!

    fun queryFirstRow(sql: String, report: Report? = null): Map<String, Any?> = queryData(sql, report).first()
}
