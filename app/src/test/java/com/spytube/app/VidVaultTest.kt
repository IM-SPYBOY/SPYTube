package com.spytube.app

import com.spytube.app.api.VidVaultClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class VidVaultTest {
    @Test
    fun testVidVaultTv() = runBlocking {
        println("Resolving TMDB ID for The Boys...")
        val tmdbId = VidVaultClient.resolveTmdbId("The Boys", false)
        println("Resolved TMDB ID: $tmdbId")
        
        assertNotNull("TMDB ID should not be null", tmdbId)
        
        if (tmdbId != null) {
            println("Fetching links for TMDB $tmdbId, Season 1, Episode 1...")
            val links = VidVaultClient.fetchLinks(
                tmdbId = tmdbId,
                type = "tv",
                season = 1,
                episode = 1,
                title = "The Boys"
            )
            println("Found ${links.size} links")
            assertTrue("Should find at least 1 link", links.isNotEmpty())
            
            for (link in links) {
                println("${link.quality} -> ${link.workerUrl ?: link.vcloudUrl}")
            }
        }
    }
}
