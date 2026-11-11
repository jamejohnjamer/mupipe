package app.mupipe.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name ASC")
    fun all(): Flow<List<GroupEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(group: GroupEntity): Long
    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY channelName ASC")
    fun all(): Flow<List<SubscriptionEntity>>
    @Query("SELECT * FROM subscriptions WHERE groupId = :groupId ORDER BY channelName ASC")
    fun byGroup(groupId: Long): Flow<List<SubscriptionEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<SubscriptionEntity>): List<Long>
    @Query("SELECT channelId FROM subscriptions")
    suspend fun allIds(): List<String>
    @Query("SELECT COUNT(*) FROM subscriptions WHERE channelId = :id")
    suspend fun exists(id: String): Int
    @Update
    suspend fun update(item: SubscriptionEntity)
    @Query("DELETE FROM subscriptions WHERE channelId = :id")
    suspend fun delete(id: String)
    @Query("UPDATE subscriptions SET lastCheckedMillis = :at, lastSeenVideoId = :lastSeen WHERE channelId = :id")
    suspend fun markChecked(id: String, at: Long, lastSeen: String?)
}

@Dao
interface UploadHistoryDao {
    @Query("SELECT * FROM upload_history ORDER BY publishedAtMillis DESC LIMIT :limit OFFSET :offset")
    suspend fun page(limit: Int, offset: Int): List<UploadHistoryEntity>
    @Query("SELECT * FROM upload_history WHERE channelId = :channelId ORDER BY publishedAtMillis DESC LIMIT 200")
    suspend fun byChannel(channelId: String): List<UploadHistoryEntity>
    @Query("SELECT COUNT(*) FROM upload_history WHERE seen = 0 AND (:groupId IS NULL OR groupId = :groupId)")
    suspend fun unseenCount(groupId: Long?): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<UploadHistoryEntity>)
    @Query("UPDATE upload_history SET seen = 1 WHERE videoId = :id")
    suspend fun markSeen(id: String)
    @Query("UPDATE upload_history SET seen = 1 WHERE groupId = :groupId")
    suspend fun markGroupSeen(groupId: Long)
    @Query("DELETE FROM upload_history WHERE firstSeenAtMillis < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long): Int
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY savedAtMillis DESC")
    fun all(): Flow<List<PlaylistEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntries(entries: List<PlaylistEntryEntity>)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :id ORDER BY position ASC")
    suspend fun entries(id: String): List<PlaylistEntryEntity>
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY updatedAtMillis DESC")
    fun all(): Flow<List<DownloadEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: DownloadEntity): Long
    @Update
    suspend fun update(item: DownloadEntity)
    @Query("SELECT * FROM downloads WHERE videoId = :id LIMIT 1")
    suspend fun byId(id: String): DownloadEntity?
    @Query("SELECT * FROM downloads WHERE state IN ('Queued','Downloading','WaitingForNetwork','Retrying') ORDER BY updatedAtMillis ASC LIMIT 10")
    suspend fun active(): List<DownloadEntity>
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY queriedAtMillis DESC LIMIT 20")
    suspend fun recent(): List<SearchHistoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SearchHistoryEntity)
    @Query("DELETE FROM search_history WHERE query = :q")
    suspend fun delete(q: String)
}

@Dao
interface PreferenceDao {
    @Query("SELECT value FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(item: PreferenceEntity)
}
