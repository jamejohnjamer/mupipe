package app.mupipe.android.data.model

/** Shared video model. Mirrors shared/schema.json (desktop). No player state. */
data class VideoItem(
    val videoId: String,
    val title: String,
    val channelId: String? = null,
    val channelName: String? = null,
    val channelIconUrl: String? = null,
    val thumbnailUrl: String? = null,
    val publishedAtMillis: Long? = null,
    val durationSeconds: Long? = null,
    val viewCount: Long? = null,
    val isLive: Boolean = false,
    val isUpcoming: Boolean = false,
    val hasCaptions: Boolean = false,
)

data class ChannelItem(
    val channelId: String,
    val name: String,
    val handle: String? = null,
    val iconUrl: String? = null,
    val subscriberCount: Long? = null,
    val description: String? = null,
)

data class PlaylistItem(
    val playlistId: String,
    val title: String,
    val channelName: String? = null,
    val videoCount: Int = 0,
    val thumbnailUrl: String? = null,
)

/** Search filters. Only values the Invidious API accepts (see research). */
data class SearchFilters(
    val type: String = "all",          // video | channel | playlist | movie | show | all
    val date: String? = null,          // hour | today | week | month | year
    val duration: String? = null,      // short | medium | long
    val sort: String = "relevance",    // relevance | upload_date | view_count
    val liveOnly: Boolean = false,     // server-side features=live
    val subtitlesOnly: Boolean = false, // server-side features=subtitles, doc-verified only
) {
    companion object {
        val DATE_OPTIONS = listOf(null, "hour", "today", "week", "month", "year")
        val DURATION_OPTIONS = listOf(null, "short", "medium", "long")
        val SORT_OPTIONS = listOf("relevance", "upload_date", "view_count")
        val TYPE_OPTIONS = listOf("all", "video", "channel", "playlist")
    }
}

enum class DownloadState {
    Queued, Downloading, WaitingForNetwork, Retrying, PausedByUser, Completed, Failed
}

data class DownloadJob(
    val videoId: String,
    val title: String,
    val url: String,
    val fileName: String,
    val audioOnly: Boolean,
    val state: DownloadState,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val retryCount: Int = 0,
)
