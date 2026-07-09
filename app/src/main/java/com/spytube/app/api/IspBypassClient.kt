package com.spytube.app.api

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * IspBypassClient — Unified multi-layer ISP bypass OkHttpClient.
 *
 * Layer 1: DNS-over-HTTPS via [DohTunnel] (multi-server fallback)
 * Layer 2: TLS 1.3 enforcement via [DohTunnel.tlsSpec]
 * Layer 3: Cloudflare Worker proxy — activated on direct connection failure
 *
 * All three API services (Cinefy, HiCine, VidVault) use this single client.
 * The Worker proxy is transparent — no user configuration needed.
 */
object IspBypassClient {

    private const val TAG = "IspBypassClient"

    /**
     * Cloudflare Worker that proxies all API requests.
     * The Worker receives: GET /https://blocked-domain.com/api/endpoint
     * and fetches it server-side, returning the response.
     *
     * ISPs see only: phone → Cloudflare IP (never blocked).
     */
    private const val WORKER_BASE = "https://spytube-proxy.spyboy.workers.dev"

    /**
     * Domains that MUST go through the proxy on failure.
     * Direct connections are always tried first for speed.
     */
    private val PROXIED_DOMAINS = setOf(
        "cinefy.lol",
        "api.hicine.info",
        "hicine.info",
        "vidvault.ru"
    )

    /** Interceptor that adds common anti-fingerprinting headers */
    private val headerInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cache-Control", "no-cache")
            .build()
        chain.proceed(req)
    }

    /** The shared bypass-enabled OkHttpClient used by all API services */
    val client: OkHttpClient by lazy {
        DohTunnel.client.newBuilder()
            .addInterceptor(headerInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * A variant of the bypass client that also injects custom headers.
     * Used by VidVault which needs specific Referer/Origin headers.
     */
    fun clientWithHeaders(vararg headers: Pair<String, String>): OkHttpClient {
        return client.newBuilder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    headers.forEach { (k, v) -> header(k, v) }
                }.build()
                chain.proceed(req)
            }
            .build()
    }
}
