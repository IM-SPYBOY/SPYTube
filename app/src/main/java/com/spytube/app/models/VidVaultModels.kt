package com.spytube.app.models

import com.google.gson.annotations.SerializedName

data class VidVaultTokenResponse(
    @SerializedName("t") val token: String
)

data class VidVaultRequest(
    @SerializedName("type") val type: String, // "movie" or "tv"
    @SerializedName("tmdbId") val tmdbId: String,
    @SerializedName("season") val season: Int? = null,
    @SerializedName("episode") val episode: Int? = null
)

data class VidVaultResponse(
    @SerializedName("extractData") val extractData: VidVaultExtractData?,
    @SerializedName("mkvData") val mkvData: VidVaultMkvData?
)

data class VidVaultExtractData(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val dataWrapper: VidVaultExtractDataWrapper?
)

data class VidVaultExtractDataWrapper(
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: VidVaultExtractInner?
)

data class VidVaultExtractInner(
    @SerializedName("streams") val streams: List<VidVaultDownload>?,
    @SerializedName("downloads") val downloads: List<VidVaultDownload>?
)

data class VidVaultDownload(
    @SerializedName("url") val url: String?,
    @SerializedName("resolution") val resolution: Int?,
    @SerializedName("size") val size: String? // raw bytes as string
)

data class VidVaultMkvData(
    @SerializedName("files") val files: List<VidVaultFile>?
)

data class VidVaultFile(
    @SerializedName("url") val url: String?,
    @SerializedName("size") val size: String?
)
