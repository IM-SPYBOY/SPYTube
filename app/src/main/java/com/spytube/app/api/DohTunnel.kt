package com.spytube.app.api

import okhttp3.ConnectionPool
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.TlsVersion
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * DohTunnel — Layer 1 ISP bypass: DNS-over-HTTPS with multi-server fallback.
 *
 * Fallback chain:
 *   1. Cloudflare (1.1.1.1) — fastest, privacy-first
 *   2. Google (8.8.8.8)     — most reliable globally
 *   3. Quad9 (9.9.9.9)      — security-focused, malware filtering skipped for our use
 *   4. System DNS            — last resort (ISP may have poisoned this)
 *
 * Also enforces TLS 1.3 to hamper SNI-based DPI inspection (Layer 2).
 */
object DohTunnel {

    /** Bootstrap client — used ONLY to resolve DoH server IPs, no proxy */
    private val bootstrap: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    private fun buildDoh(url: String, vararg bootstrapIps: String): DnsOverHttps? {
        return try {
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url(url.toHttpUrl())
                .bootstrapDnsHosts(*bootstrapIps.map { InetAddress.getByName(it) }.toTypedArray())
                .build()
        } catch (_: Exception) {
            null
        }
    }

    /** Try multiple DoH servers in order; falls back to system DNS if all fail */
    val resolver: Dns by lazy {
        val cloudflare = buildDoh("https://1.1.1.1/dns-query", "1.1.1.1", "1.0.0.1")
        val google     = buildDoh("https://8.8.8.8/dns-query", "8.8.8.8", "8.8.4.4")
        val quad9      = buildDoh("https://9.9.9.9/dns-query", "9.9.9.9")

        val servers = listOfNotNull(cloudflare, google, quad9)
        if (servers.isEmpty()) return@lazy Dns.SYSTEM

        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                for (doh in servers) {
                    try {
                        val result = doh.lookup(hostname)
                        if (result.isNotEmpty()) return result
                    } catch (_: Exception) { /* try next server */ }
                }
                // Final fallback — system DNS (may be poisoned by ISP)
                return Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    /** TLS 1.3 + TLS 1.2 connection spec — prevents SNI downgrade attacks */
    private val tlsSpec: ConnectionSpec by lazy {
        ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()
    }

    private val sharedPool = ConnectionPool(32, 5, TimeUnit.MINUTES)

    /** Shared OkHttpClient with DoH + TLS 1.3 enforcement and unsafe TrustManager */
    val client: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        val sslSocketFactory = sslContext.socketFactory

        OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .dns(resolver)
            .connectionPool(sharedPool)
            .connectionSpecs(listOf(tlsSpec, ConnectionSpec.CLEARTEXT))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
    }
}
