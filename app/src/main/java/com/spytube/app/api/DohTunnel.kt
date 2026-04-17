package com.spytube.app.api

import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object DohTunnel {

    private val dnsResolver: Dns by lazy {
        try {
            val bootstrap = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            DnsOverHttps.Builder()
                .client(bootstrap)
                .url("https://1.1.1.1/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("8.8.8.8")
                )
                .build()
        } catch (_: Exception) {
            Dns.SYSTEM
        }
    }

    private val sharedPool = ConnectionPool(32, 5, TimeUnit.MINUTES)

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dnsResolver)
            .connectionPool(sharedPool)
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            .build()
    }
}
