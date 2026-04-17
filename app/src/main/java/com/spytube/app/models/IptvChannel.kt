package com.spytube.app.models


/** Merged channel from M3U — ready to display & play */
data class IptvChannel(
    val id: String,
    val name: String,
    val country: String,
    val categories: List<String>,
    val streamUrl: String,
    val quality: String?,
    val referrer: String?,
    val userAgent: String?,
    val logoUrl: String?
) : java.io.Serializable

/** Category for UI display */
data class IptvCategory(
    val id: String,
    val name: String,
    val emoji: String = ""
)
