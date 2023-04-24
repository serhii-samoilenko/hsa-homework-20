package com.example

import com.example.util.Report

/**
 * Database: Replication demo
 */
fun runDemo() {
    val r = Report("REPORT.md")
    r.h1("Database: Replication report")

    r.writeToFile()
}
