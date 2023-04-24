package com.example.util

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.Closeable

class Docker(private val r: Report) {
    fun start(service: Service): Closeable {
        r.text(
            "Docker container: `${service.container.dockerImageName}`, command: `${service.container.commandParts.toList()}`",
        )
        service.container.start()
        return Closeable {
            service.container.stop()
            println("Stopped Docker container: `${service.container.dockerImageName}`")
        }
    }

    enum class Service(val container: GenericContainer<*>) {
        REDIS_NOP(
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withCommand("redis-server", "--save", "", "--appendonly", "no")
                .withReuse(true)
                .also { it.setPortBindings(listOf("6379:6379")) },
        ),
        REDIS_AOF(
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withCommand("redis-server", "--save", "", "--appendonly", "yes")
                .withReuse(true)
                .also { it.setPortBindings(listOf("6379:6379")) },
        ),
        REDIS_RDB(
            GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withCommand("redis-server", "--save", "5 1000", "--save", "1 100", "--appendonly", "no")
                .withReuse(true)
                .also { it.setPortBindings(listOf("6379:6379")) },
        ),
        BEANSTALKD_NOP(
            GenericContainer(DockerImageName.parse("uretgec/beanstalkd-alpine"))
                .withCommand("-F", "-z", maxMessageSize)
                .withReuse(true)
                .also { it.setPortBindings(listOf("11300:11300")) },
        ),
        BEANSTALKD_0S(
            GenericContainer(DockerImageName.parse("uretgec/beanstalkd-alpine"))
                .withCommand("-b", "/data", "-f", "0", "-z", maxMessageSize)
                .withReuse(true)
                .also { it.setPortBindings(listOf("11300:11300")) },
        ),
        BEANSTALKD_1S(
            GenericContainer(DockerImageName.parse("uretgec/beanstalkd-alpine"))
                .withCommand("-b", "/data", "-f", "1000", "-z", maxMessageSize)
                .withReuse(true)
                .also { it.setPortBindings(listOf("11300:11300")) },
        ),
    }

    companion object {
        private const val maxMessageSize = (1024 * 1024 * 10).toString()
    }
}
