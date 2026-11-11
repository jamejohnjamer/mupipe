package app.mupipe.android.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "groups", indices = [Index(value = ["name"], unique = true)])
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["channelId"], unique = true), Index("groupId")]
)
data class SubscriptionEntity(
    @PrimaryKey val channelId: String,
    val channelName: String,
    val handle: String? = null,
    val iconUrl: String? = null,
    val groupId: Long? = null,
    val subscribedAtMillis: Long,
    val lastCheckedMillis: Long? = null,
    val lastSeenVideoId: String? = null,
)

@Entity(
    tableName = "upload_history",
    indices = [Index("channelId"), Index("groupId"), Index("publishedAtMillis"), Index("firstSeenAtMillis")]
)
data class UploadHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val groupId: Long? = null,
    val publishedAtMillis: Long? = null,
    val firstSeenAtMillis: Long,
    val seen: Boolean = false,
    val available: Boolean = true,
)

@Entity(tableName = "playlists", indices = [Index(value = ["playlistId"], unique = true)])
data class PlaylistEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val channelName: String? = null,
    val videoCount: Int = 0,
    val savedAtMillis: Long,
)

@Entity(
    tableName = "playlist_entries",
    primaryKeys = ["playlistId", "videoId"],
    indices = [Index("videoId")]
)
data class PlaylistEntryEntity(
    val playlistId: String,
    val videoId: String,
    val title: String,
    val position: Int,
    val durationSeconds: Long? = null,
)

@Entity(tableName = "downloads", indices = [Index("state")])
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val url: String,
    val fileName: String,
    val audioOnly: Boolean,
    val state: String, // DownloadState name
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val retryCount: Int = 0,
    val updatedAtMillis: Long,
)

@Entity(tableName = "search_history", indices = [Index("queriedAtMillis")])
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val queriedAtMillis: Long,
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String,
)
