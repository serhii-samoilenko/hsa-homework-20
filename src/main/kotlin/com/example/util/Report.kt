package com.example.util

import java.io.File
import java.lang.Integer.max

class Report(
    private val fileName: String,
) {
    private val targetDir = "reports"
    private var data: String = ""

    @Synchronized
    fun text(text: String) {
        "$text\n".also { data += it + "\n" }.also { println(it) }
    }

    @Synchronized
    fun line(text: String) {
        text.also { data += it + "\n" }.also { println(it) }
    }

    fun h1(text: String) {
        text("# $text")
    }

    fun h2(text: String) {
        text("## $text")
    }

    fun h3(text: String) {
        text("### $text")
    }

    fun h4(text: String) {
        text("#### $text")
    }

    fun code(text: String) {
        text("`$text`")
    }

    fun line() {
        text("---")
    }

    fun json(json: String) {
        val block = "```json\n${json}\n```"
        text(block)
    }

    fun block(block: String) {
        text("```\n$block\n```")
    }

    fun sql(sql: String) {
        sql(listOf(sql))
    }

    fun sql(sql: List<String>) {
        val block = "```sql\n${sql.joinToString(";\n")};\n```"
        text(block)
    }

    fun sql(sql: List<String>, actor: String) {
        sql(sql, actor, null)
    }

    fun sql(sql: List<String>, actor: String, result: String?) {
        var block = "```sql\n"
        if (actor.isNotBlank()) {
            block += "-- $actor:\n"
        }
        block += sql.joinToString(";\n")
        if (result?.isNotBlank() == true) {
            block += "\n-- Result: $result"
        }
        block += "\n```"
        text(block)
    }

    @Synchronized
    fun writeToFile() {
        File(targetDir).mkdirs()
        val file = File(targetDir, fileName)
        file.writeText(data)
        println("Wrote report to ${file.absolutePath}")
    }

    @Synchronized
    fun clear() {
        data = ""
    }

    fun table(titleOne: String, titleTwo: String, data: List<Pair<String, String>>) {
        val maxLengthQuery = max(titleOne.length, data.maxByOrNull { it.first.length }?.first?.length ?: 0)
        val maxLengthResult = max(titleTwo.length, data.maxByOrNull { it.second.length }?.second?.length ?: 0)

        line("| $titleOne${" ".repeat(maxLengthQuery - titleOne.length)} | $titleTwo${" ".repeat(maxLengthResult - titleTwo.length)} |")
        line("|${"-".repeat(maxLengthQuery + 2)}|${"-".repeat(maxLengthResult + 2)}|")

        data.forEach { (query, result) ->
            val paddedQuery = query.padEnd(maxLengthQuery + 1)
            val paddedResult = result.padEnd(maxLengthResult + 1)
            line("| $paddedQuery| $paddedResult|")
        }
        line("")
    }
}
