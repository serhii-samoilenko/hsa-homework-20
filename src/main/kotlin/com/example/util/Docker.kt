package com.example.util

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import java.io.File

class Docker {
    private val config: DockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
        .withDockerHost("unix:///var/run/docker.sock")
        .withDockerTlsVerify(false)
        .build()
    private val httpClient = ZerodepDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
    private val dockerClient = DockerClientImpl.getInstance(config, httpClient)

    fun isRunningInDocker() = File("/.dockerenv").exists()

    fun startContainer(containerName: String) {
        println("Starting $containerName...")
        dockerClient.startContainerCmd(containerName).exec()
        waitForContainer(containerName, 60)
    }

    fun stopContainer(containerName: String) {
        println("Stopping $containerName...")
        dockerClient.stopContainerCmd(containerName).exec()
    }

    fun waitForContainer(containerName: String, timeout: Int = 60) {
        print("Waiting for $containerName to become healthy...")
        var count = 0
        while (true) {
            if (count > timeout) {
                println(" Fail")
                throw Exception("Timeout waiting for $containerName to become healthy")
            }
            if (waitForContainer(containerName)) {
                println(" OK")
                return
            } else {
                print('.')
                Thread.sleep(1000)
                count++
            }
        }
    }

    private fun waitForContainer(containerName: String): Boolean {
        val containerResponse = dockerClient.inspectContainerCmd(containerName).exec()
        if (containerResponse.state.health != null) {
            containerResponse.state.health?.let { health ->
                if (health.status == "healthy") {
                    return true
                }
            }
        } else {
            containerResponse.state.status?.let { status ->
                if (status == "running") {
                    return true
                }
            }
        }
        return false
    }
}
