package app.mupipe.android.data

import app.mupipe.android.data.db.GroupEntity
import app.mupipe.android.data.db.SubscriptionEntity
import app.mupipe.android.data.db.UploadHistoryEntity
import app.mupipe.android.data.model.ChannelItem
import app.mupipe.android.data.model.PlaylistItem
import app.mupipe.android.data.model.SearchFilters
import app.mupipe.android.data.model.VideoItem
import app.mupipe.android.data.provider.RssEntry
import app.mupipe.android.data.provider.RssParser
import app.mupipe.android.data.provider.SearchFilterMapper
import app.mupipe.android.util.ImportParser
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Repository: all network + DB work, always on IO dispatcher.
 * UI never calls a provider directly. Failures say which provider failed.
 */
class Repository(private val container: AppContainer) {
    private val db get() = container.db
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    suspend fun search(query: String, filters: SearchFilters, page: Int = 1): Result<List<VideoItem>> =
        withContext(container.ioDispatcher) {
            runCatching {
                val params = SearchFilterMapper.toParams(filters)
                val api = container.invidiousApi()
                val raw = api.search(
                    q = query, page = page,
                    type = params["type"] ?: "all",
                    sortBy = params["sort_by"] ?: "relevance",
                    date = params["date"], duration = params["duration"],
                    features = params["features"],
                )
                raw.mapNotNull { v ->
                    val id = v.videoId ?: return@mapNotNull null
                    VideoItem(
                        videoId = id, title = v.title ?: "",
                        channelId = v.authorId, channelName = v.author,
                        channelIconUrl = v.authorThumbnails?.lastOrNull()?.url,
                        thumbnailUrl = v.thumbnails?.lastOrNull()?.url,
                        publishedAtMillis = v.published?.let { if (it < 10_000_000_000L) it * 1000 else it },
                        durationSeconds = v.lengthSeconds, viewCount = v.viewCount,
                        isLive = v.liveNow, isUpcoming = v.isUpcoming,
                    )
                }
            }
        }

    suspend fun suggestions(query: String): Result<List<String>> =
        withContext(container.ioDispatcher) { runCatching { container.invidiousApi().suggestions(query) } }

    suspend fun trending(): Result<List<VideoItem>> =
        withContext(container.ioDispatcher) {
            runCatching {
                container.invidiousApi().trending().mapNotNull { v ->
                    val id = v.videoId ?: return@mapNotNull null
                    VideoItem(id, v.title ?: "", v.authorId, v.author, null, v.thumbnails?.lastOrNull()?.url, null, v.lengthSeconds, v.viewCount, v.liveNow)
                }
            }
        }

    suspend fun channel(id: String): Result<ChannelItem> =
        withContext(container.ioDispatcher) {
            runCatching {
                val c = container.invidiousApi().channel(id)
                ChannelItem(c.authorId ?: id, c.author ?: "", null, c.authorThumbnails?.lastOrNull()?.url, c.subCount, c.description)
            }
        }

    suspend fun playlist(id: String): Result<PlaylistItem> =
        withContext(container.ioDispatcher) {
            runCatching {
                val p = container.invidiousApi().playlist(id)
                PlaylistItem(p.playlistId ?: id, p.title ?: "", p.author, p.videoCount)
            }
        }

    /** RSS refresh for one channel. Returns new entries only (dedup by videoId). */
    suspend fun refreshChannelRss(channelId: String): Result<List<RssEntry>> =
        withContext(container.ioDispatcher) {
            runCatching {
                val req = Request.Builder().url(RssParser.feedUrlForChannel(channelId)).build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("RSS failed: HTTP ${resp.code}")
                    RssParser.parse(resp.body?.string().orEmpty())
                }
            }
        }

    /** Resolve a @handle or channel URL to a channel ID via Invidious search. */
    suspend fun resolveHandle(handle: String): Result<String> =
        withContext(container.ioDispatcher) {
            runCatching {
                val clean = handle.removePrefix("@")
                val results = container.invidiousApi().search(clean, type = "channel")
                // Invidious channel search returns authorId in the video-shaped rows.
                results.firstNotNullOfOrNull { it.authorId } ?: error("Handle not found: $handle")
            }
        }

    suspend fun importChannels(text: String, groupId: Long?): ImportResult =
        withContext(container.ioDispatcher) {
            val entries = ImportParser.parseImportEntries(text)
            val existingIds = db.subscriptionDao().allIds()
            val (duplicates, fresh) = ImportParser.findDuplicates(entries, existingIds)
            val resolved = mutableListOf<SubscriptionEntity>()
            val unresolved = mutableListOf<String>()
            for (e in fresh) {
                when (e) {
                    is ImportParser.Entry.ChannelId -> resolved.add(
                        SubscriptionEntity(e.channelId, e.channelId, null, null, groupId, System.currentTimeMillis()))
                    is ImportParser.Entry.Handle -> {
                        val id = resolveHandle(e.handle).getOrNull()
                        if (id == null) unresolved.add(e.raw)
                        else resolved.add(SubscriptionEntity(id, e.handle, e.handle, null, groupId, System.currentTimeMillis()))
                    }
                    is ImportParser.Entry.ChannelUrl -> {
                        val id = e.channelId ?: e.handle?.let { resolveHandle(it).getOrNull() }
                        if (id == null) unresolved.add(e.raw)
                        else resolved.add(SubscriptionEntity(id, e.handle ?: id, e.handle, null, groupId, System.currentTimeMillis()))
                    }
                    else -> unresolved.add(e.raw)
                }
            }
            if (resolved.isNotEmpty()) db.subscriptionDao().insertAll(resolved)
            ImportResult(imported = resolved.size, duplicates = duplicates.map { it.raw }, unresolved = unresolved)
        }

    data class ImportResult(val imported: Int, val duplicates: List<String>, val unresolved: List<String>)

    suspend fun recordUploads(channelId: String, channelName: String, groupId: Long?, entries: List<RssEntry>) {
        withContext(container.ioDispatcher) {
            val now = System.currentTimeMillis()
            db.uploadHistoryDao().insertAll(entries.map {
                UploadHistoryEntity(it.videoId, it.title, channelId, channelName, groupId, it.publishedMillis, now)
            })
            db.uploadHistoryDao().pruneOlderThan(now - 30L * 24 * 60 * 60 * 1000)
        }
    }

    suspend fun createGroup(name: String): Long =
        withContext(container.ioDispatcher) {
            db.groupDao().insert(GroupEntity(name = name.trim(), createdAtMillis = System.currentTimeMillis()))
        }

    suspend fun markGroupSeen(groupId: Long) =
        withContext(container.ioDispatcher) { db.uploadHistoryDao().markGroupSeen(groupId) }
}
