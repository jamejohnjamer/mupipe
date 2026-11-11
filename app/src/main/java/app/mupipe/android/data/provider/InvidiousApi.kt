package app.mupipe.android.data.provider

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** DTOs for the public Invidious API (api/v1). Only fields MuPipe shows. */
data class InvidiousThumbnail(val url: String? = null, val width: Int? = null, val height: Int? = null)
data class InvidiousVideo(
    @SerializedName("videoId") val videoId: String? = null,
    val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("authorId") val authorId: String? = null,
    @SerializedName("authorThumbnails") val authorThumbnails: List<InvidiousThumbnail>? = null,
    @SerializedName("viewCount") val viewCount: Long? = null,
    @SerializedName("lengthSeconds") val lengthSeconds: Long? = null,
    val published: Long? = null,
    @SerializedName("publishedText") val publishedText: String? = null,
    @SerializedName("liveNow") val liveNow: Boolean = false,
    @SerializedName("isUpcoming") val isUpcoming: Boolean = false,
    val thumbnails: List<InvidiousThumbnail>? = null,
    val type: String? = null,
)
data class InvidiousChannel(
    @SerializedName("authorId") val authorId: String? = null,
    val author: String? = null,
    @SerializedName("authorThumbnails") val authorThumbnails: List<InvidiousThumbnail>? = null,
    @SerializedName("subCount") val subCount: Long? = null,
    val description: String? = null,
    @SerializedName("latestVideos") val latestVideos: List<InvidiousVideo>? = null,
)
data class InvidiousPlaylistVideo(
    @SerializedName("videoId") val videoId: String? = null,
    val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("lengthSeconds") val lengthSeconds: Long? = null,
)
data class InvidiousPlaylist(
    @SerializedName("playlistId") val playlistId: String? = null,
    val title: String? = null,
    val author: String? = null,
    @SerializedName("videoCount") val videoCount: Int = 0,
    val videos: List<InvidiousPlaylistVideo>? = null,
)
data class InvidiousFormatStream(
    val url: String? = null,
    val itag: Int? = null,
    val type: String? = null,
    val quality: String? = null,
    @SerializedName("qualityLabel") val qualityLabel: String? = null,
)
data class InvidiousVideoDetails(
    @SerializedName("videoId") val videoId: String? = null,
    val title: String? = null,
    @SerializedName("formatStreams") val formatStreams: List<InvidiousFormatStream>? = null,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<InvidiousFormatStream>? = null,
    @SerializedName("hlsUrl") val hlsUrl: String? = null,
    @SerializedName("dashUrl") val dashUrl: String? = null,
    @SerializedName("isUpcoming") val isUpcoming: Boolean = false,
    @SerializedName("liveNow") val liveNow: Boolean = false,
)

/** Retrofit service. Base URL is set per healthy instance at runtime. */
interface InvidiousApi {
    @GET("api/v1/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int = 1,
        @Query("type") type: String = "all",
        @Query("sort_by") sortBy: String = "relevance",
        @Query("date") date: String? = null,
        @Query("duration") duration: String? = null,
        @Query("features") features: String? = null,
        @Query("region") region: String = "US",
    ): List<InvidiousVideo>

    @GET("api/v1/search/suggestions")
    suspend fun suggestions(@Query("q") q: String): List<String>

    @GET("api/v1/trending")
    suspend fun trending(@Query("type") type: String = "Default"): List<InvidiousVideo>

    @GET("api/v1/channels/{id}")
    suspend fun channel(@Path("id") id: String): InvidiousChannel

    @GET("api/v1/channels/{id}/videos")
    suspend fun channelVideos(
        @Path("id") id: String,
        @Query("sort_by") sortBy: String = "newest",
    ): List<InvidiousVideo>

    @GET("api/v1/playlists/{id}")
    suspend fun playlist(@Path("id") id: String): InvidiousPlaylist

    @GET("api/v1/videos/{id}")
    suspend fun video(@Path("id") id: String): InvidiousVideoDetails
}

/** Verified live 2026-11-10: only yt.chocolatemoo53.com had API on. */
object InvidiousInstances {
    val DEFAULTS = listOf(
        "https://yt.chocolatemoo53.com",
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://invidious.tiekoetter.com",
    )
    const val INSTANCES_JSON = "https://api.invidious.io/instances.json?sort_by=type,health"
}
