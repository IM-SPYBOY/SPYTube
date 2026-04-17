package com.spytube.app.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ── HiCine API Response Models ─────────────────────────────────────


data class HiCineItem(
    @SerializedName("_id") val id: String,
    @SerializedName("record_id") val recordId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("featured_image") val featuredImage: String?,
    @SerializedName("links") val links: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("url_slug") val urlSlug: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("modified_date") val modifiedDate: String?,
    @SerializedName("excerpt") val excerpt: String?,
    @SerializedName("cloudlinks") val cloudlinks: String?,
    @SerializedName("contentType") val contentType: String?,

    // Series season fields
    @SerializedName("season_1") val season1: String?,
    @SerializedName("season_2") val season2: String?,
    @SerializedName("season_3") val season3: String?,
    @SerializedName("season_4") val season4: String?,
    @SerializedName("season_5") val season5: String?,
    @SerializedName("season_6") val season6: String?,
    @SerializedName("season_7") val season7: String?,
    @SerializedName("season_8") val season8: String?,
    @SerializedName("season_9") val season9: String?,
    @SerializedName("season_10") val season10: String?,
    @SerializedName("season_zip") val seasonZip: String?
) : Serializable {

    /** True if this is a series (has any season data) */
    val isSeries: Boolean
        get() = listOf(season1, season2, season3, season4, season5,
            season6, season7, season8, season9, season10)
            .any { !it.isNullOrBlank() }

    /** Get season data by number (1-indexed) */
    fun getSeasonData(seasonNumber: Int): String? = when (seasonNumber) {
        1 -> season1; 2 -> season2; 3 -> season3; 4 -> season4; 5 -> season5
        6 -> season6; 7 -> season7; 8 -> season8; 9 -> season9; 10 -> season10
        else -> null
    }

    /** Count available seasons */
    val seasonCount: Int
        get() = (1..10).count { getSeasonData(it) != null }
}


data class HiCinePaginatedResponse(
    @SerializedName("data") val data: List<HiCineItem>,
    @SerializedName("pagination") val pagination: HiCinePagination?
)

data class HiCinePagination(
    @SerializedName("page") val page: Int,
    @SerializedName("limit") val limit: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("pages") val pages: Int?,
    @SerializedName("totalPages") val totalPages: Int?,
    @SerializedName("hasMore") val hasMore: Boolean?
)

// ── Download Link Resolution Models ────────────────────────────────


data class HiCineDownloadLink(
    val workerUrl: String,    // For HiCine: worker domain. For VidVault: the direct MKV URL.
    val vcloudUrl: String,    // For HiCine: the vcloud identifier.
    val quality: String,      // e.g. "480p", "720p", "1080p"
    val size: String,         // e.g. "340MB", "1.2GB"
    val description: String,  // Full description line
    val episodeNumber: Int? = null, // For series episodes
    val source: String = "HiCine" // "HiCine" or "VidVault"
) : Serializable


data class HiCineTokenResponse(
    @SerializedName("title") val title: String?,
    @SerializedName("size") val size: String?,
    @SerializedName("tokens") val tokens: Map<String, HiCineTokenPair>?
)

data class HiCineTokenPair(
    @SerializedName("ts") val ts: String,
    @SerializedName("sig") val sig: String
)
