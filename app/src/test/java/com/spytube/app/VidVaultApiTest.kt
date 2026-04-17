package com.spytube.app

import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.spytube.app.api.VidVaultClient

class VidVaultApiTest {
    @Test
    fun testFetch() = runBlocking {
        println("STARTING TEST PING")
        try {
            val res = VidVaultClient.fetchLinks("76479", "tv", 1, 1, "The Boys")
            println("FINAL RESULTS SIZE: " + res.size)
            println("LAST ERROR: " + VidVaultClient.lastError)
            res.forEach { println("LINK: " + it.workerUrl) }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}
