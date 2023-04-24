package com.example

import com.example.util.ConnectionPool
import com.example.util.Database
import com.example.util.Docker
import com.example.util.Report
import java.util.UUID
import java.util.concurrent.Semaphore

/**
 * Database: Replication demo
 */
fun runDemo() {
    val r = Report("REPORT.md")
    val docker = Docker()
    r.h1("Database: Replication report")
    r.text("Testing the behavior of a database with replication.")

    r.h2("Setting up databases and replication")
    r.text("Starting the master and two slaves.")

    r.text("Preparing the master database:")
    docker.waitForContainer("mysql-master")
    val master = Database(ConnectionPool("jdbc:mysql://localhost:3306/test", "root", "root"))

    master.execute(
        "CREATE USER IF NOT EXISTS 'slave_one'@'%' IDENTIFIED BY 'slave'",
        "CREATE USER IF NOT EXISTS 'slave_two'@'%' IDENTIFIED BY 'slave'",
        "GRANT REPLICATION SLAVE ON *.* TO 'slave_one'@'%'",
        "GRANT REPLICATION SLAVE ON *.* TO 'slave_two'@'%'",
        "FLUSH PRIVILEGES",
        report = r,
    )
    val masterStatus = master.queryFirstRow("SHOW MASTER STATUS")
    r.text("Master status:")
    r.block(masterStatus.prettify())
    val masterLog = masterStatus["File"]!!
    val masterPos = masterStatus["Position"]!!

    r.text("Preparing first slave:")
    docker.waitForContainer("mysql-slave-one")
    val slaveOne = Database(ConnectionPool("jdbc:mysql://localhost:3307/test", "root", "root"))
    slaveOne.execute(
        "STOP SLAVE",
        "DROP TABLE IF EXISTS test",
        """
            CHANGE MASTER TO MASTER_HOST='mysql-master',
            MASTER_USER='slave_one',
            MASTER_PASSWORD='slave',
            MASTER_LOG_FILE='$masterLog',
            MASTER_LOG_POS=$masterPos
        """.trimIndent(),
        "START SLAVE",
        report = r,
    )

    r.text("Preparing second slave:")
    docker.waitForContainer("mysql-slave-two")
    val slaveTwo = Database(ConnectionPool("jdbc:mysql://localhost:3308/test", "root", "root"))
    slaveTwo.execute(
        "STOP SLAVE",
        "DROP TABLE IF EXISTS test",
        """
            CHANGE MASTER TO MASTER_HOST='mysql-master',
            MASTER_USER='slave_two',
            MASTER_PASSWORD='slave',
            MASTER_LOG_FILE='$masterLog',
            MASTER_LOG_POS=$masterPos
        """.trimIndent(),
        "START SLAVE",
        report = r,
    )

    r.text("Inserting data into the master:")
    master.execute(
        """
            CREATE TABLE IF NOT EXISTS test.users (
            id INT NOT NULL AUTO_INCREMENT,
            name VARCHAR(45) NOT NULL,
            PRIMARY KEY (id));
        """.trimIndent(),
        "TRUNCATE TABLE test.users",
        report = r,
    )

    r.text("Starting daemon to constantly populate master:")

    val insert = {
        master.execute("INSERT INTO test.users (name) VALUES ('${UUID.randomUUID()}')")
    }

    val runInserts = Semaphore(1)

    Thread {
        while (!Thread.interrupted()) {
            if (runInserts.tryAcquire()) {
                insert()
                runInserts.release()
            }
            Thread.sleep(100)
        }
    }.also { it.isDaemon = true }.start()

    Thread.sleep(3000)

    r.h2("Checking replication")

    r.text("Checking slave statuses:")
    r.text("Slave one:")
    r.block(slaveOne.queryFirstRow("SHOW SLAVE STATUS").prettify())
    r.text("Slave two:")
    r.block(slaveTwo.queryFirstRow("SHOW SLAVE STATUS").prettify())

    fun checkReplicationData(vararg connections: Pair<String, Database>) {
        r.text("Checking slave data:")
        runInserts.acquire()
        Thread.sleep(1000)

        connections.map { (name, connection) ->
            val count = connection.querySingleValue("SELECT COUNT(*) FROM test.users")
            r.text("$name count: `$count`")
            count
        }.distinct().let { counts ->
            if (counts.size == 1) {
                r.text("Slave data is in sync")
            } else {
                r.text("**Slave data is not in sync**")
            }
        }
        runInserts.release()
    }

    checkReplicationData(
        "Master" to master,
        "Slave one" to slaveOne,
        "Slave two" to slaveTwo,
    )

    r.h2("Turning off one of the slaves")
    r.text("Stopping slave two:")
    docker.stopContainer("mysql-slave-two")

    Thread.sleep(3000)
    checkReplicationData(
        "Master" to master,
        "Slave one" to slaveOne,
    )

    r.text("Starting slave two again:")
    docker.startContainer("mysql-slave-two")
    Thread.sleep(3000)

    checkReplicationData(
        "Master" to master,
        "Slave one" to slaveOne,
        "Slave two" to slaveTwo,
    )

    r.h2("Trying to remove column on the read-write slave")

    fun removeColumn(connection: Database) {
        r.text("Removing column on slave:")
        val exception = connection.tryExecute("ALTER TABLE test.users DROP COLUMN name", report = r)

        if (exception != null) {
            r.text("Exception: `$exception`")
        } else {
            r.text("No exception")
            r.text("Table description:")
            r.block(connection.queryFirstRow("DESCRIBE test.users").prettify())
        }
    }

    r.h3("On the read-write slave")
    removeColumn(slaveOne)

    r.h3("On the read-only slave")
    val slaveTwoUnprivileged = Database(ConnectionPool("jdbc:mysql://localhost:3308/test", "slave-two", "slave"))
    removeColumn(slaveTwoUnprivileged)

    r.h3("Final Replication status")
    checkReplicationData(
        "Master" to master,
        "Slave one" to slaveOne,
        "Slave two" to slaveTwo,
    )

    r.writeToFile()
}

private fun Map<String, Any?>.prettify() =
    this.entries
        .filter { "${it.value}".isNotBlank() }
        .sortedBy { it.key }
        .joinToString("\n")
