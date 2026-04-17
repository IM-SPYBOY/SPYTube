package com.spytube.app.models

import com.google.gson.annotations.SerializedName
import java.io.Serializable

// ── Home endpoint ──────────────────────────────────────────────
data class CinefyHomeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<CinefyCategory>?
)

data class CinefyCategory(
    @SerializedName("name") val name: String,
    @SerializedName("results") val results: List<CinefyItem>?
) : Serializable

data class CinefyItem(
    @SerializedName("id") val id: String,
    @SerializedName("posterUrl") val posterUrl: String?,
    @SerializedName("title") val title: String? = null,
    @SerializedName("year") val year: String? = null
) : Serializable

// ── Detail/Load endpoint ───────────────────────────────────────
data class CinefyDetailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: CinefyDetail?
)

data class CinefyDetail(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("isMovie") val isMovie: Boolean,
    @SerializedName("posterUrl") val posterUrl: String?,
    @SerializedName("backdropUrl") val backdropUrl: String?,
    @SerializedName("episodes") val episodes: List<CinefyEpisode>?,
    @SerializedName("defaultSeasonId") val defaultSeasonId: String?,
    @SerializedName("seasons") val seasons: List<CinefySeason>?
)

data class CinefyEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("episode") val episode: String?,
    @SerializedName("season") val season: String?,
    @SerializedName("posterUrl") val posterUrl: String?
)

data class CinefySeason(
    @SerializedName("id") val id: String,
    @SerializedName("label") val label: String?
)

// ── Episodes endpoint ──────────────────────────────────────────
data class CinefyEpisodesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: List<CinefyEpisode>?
)
