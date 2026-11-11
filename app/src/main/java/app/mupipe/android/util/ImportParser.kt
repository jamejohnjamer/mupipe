package app.mupipe.android.util

/**
 * Bulk import parser — Kotlin port of shared/logic.js parseImportEntries.
 * Same rules: split on newline/comma/semicolon, same regexes, same types.
 * Do not change the rules here without changing shared/logic.js too.
 */
object ImportParser {
    private val CHANNEL_ID_RE = Regex("""^UC[0-9A-Za-z_-]{22}$""")
    private val HANDLE_RE = Regex("""^@([A-Za-z0-9._-]+)$""")
    private val PLAYLIST_ID_RE = Regex("""^(PL|UU|OLAK|RD|LL|FL)[0-9A-Za-z_-]{10,}$""")
    private val VIDEO_ID_RE = Regex("""^[0-9A-Za-z_-]{11}$""")

    sealed class Entry {
        abstract val raw: String
        data class ChannelId(override val raw: String, val channelId: String) : Entry()
        data class Handle(override val raw: String, val handle: String) : Entry()
        data class ChannelUrl(override val raw: String, val channelId: String? = null, val handle: String? = null) : Entry()
        data class PlaylistId(override val raw: String, val playlistId: String) : Entry()
        data class VideoUrl(override val raw: String, val videoId: String, val playlistId: String? = null) : Entry()
        data class Unknown(override val raw: String) : Entry()
    }

    fun parseImportEntries(text: String?): List<Entry> {
        if (text.isNullOrBlank()) return emptyList()
        return text.split(Regex("[\\n,;]+")).map { it.trim() }.filter { it.isNotEmpty() }
            .map { parseOne(it.trimEnd('.', ',', ';', ')', ']')) }
    }

    private fun parseOne(raw: String): Entry {
        if (CHANNEL_ID_RE.matches(raw)) return Entry.ChannelId(raw, raw)
        if (HANDLE_RE.matches(raw)) return Entry.Handle(raw, raw)
        if (PLAYLIST_ID_RE.matches(raw)) return Entry.PlaylistId(raw, raw)
        val lower = raw.lowercase()
        if (lower.startsWith("http") || lower.startsWith("www.") ||
            lower.contains("youtube.com") || lower.contains("youtu.be") ||
            lower.contains("/watch") || lower.contains("/channel/")
        ) return parseUrl(raw)
        return Entry.Unknown(raw)
    }

    private fun parseUrl(raw: String): Entry {
        val withScheme = if (raw.startsWith("http", ignoreCase = true)) raw else "https://$raw"
        // Pure java.net.URI on purpose: this parser is unit-tested on the JVM
        // and must not depend on android.net.Uri.
        val uri = runCatching { java.net.URI(withScheme) }.getOrNull()
            ?: return Entry.Unknown(raw)
        val path = uri.path ?: ""
        val queryParams = uri.rawQuery?.split("&")?.mapNotNull {
            val i = it.indexOf('=')
            if (i < 0) null else it.substring(0, i) to java.net.URLDecoder.decode(it.substring(i + 1), "UTF-8")
        }?.toMap().orEmpty()
        val channelMatch = Regex("""^/channel/(UC[0-9A-Za-z_-]+)""").find(path)
        if (channelMatch != null) return Entry.ChannelUrl(raw, channelId = channelMatch.groupValues[1])
        val handleMatch = Regex("""^/@([A-Za-z0-9._-]+)""").find(path)
        if (handleMatch != null) return Entry.ChannelUrl(raw, handle = "@" + handleMatch.groupValues[1])
        if (Regex("""^/(c|user)/[^/]+$""").matches(path)) return Entry.ChannelUrl(raw)
        val list = queryParams["list"]
        if (path == "/playlist" && list != null) {
            return if (PLAYLIST_ID_RE.matches(list)) Entry.PlaylistId(raw, list) else Entry.Unknown(raw)
        }
        val v = queryParams["v"]
        if (v != null && VIDEO_ID_RE.matches(v)) {
            return Entry.VideoUrl(raw, v, if (list != null && PLAYLIST_ID_RE.matches(list)) list else null)
        }
        val shorts = Regex("""^/(shorts|live|embed|v)/([0-9A-Za-z_-]{11})""").find(path)
        if (shorts != null) return Entry.VideoUrl(raw, shorts.groupValues[2])
        if (uri.host?.lowercase() == "youtu.be") {
            val id = path.trimStart('/')
            if (VIDEO_ID_RE.matches(id)) return Entry.VideoUrl(raw, id)
        }
        return Entry.Unknown(raw)
    }

    /** Case-insensitive duplicate check, same as shared/logic.js findDuplicates. */
    fun findDuplicates(entries: List<Entry>, existingIds: Collection<String>): Pair<List<Entry>, List<Entry>> {
        val seen = existingIds.map { it.lowercase() }.toMutableSet()
        val duplicates = mutableListOf<Entry>()
        val fresh = mutableListOf<Entry>()
        for (e in entries) {
            val key = when (e) {
                is Entry.ChannelId -> e.channelId
                is Entry.Handle -> e.handle
                is Entry.ChannelUrl -> e.channelId ?: e.handle ?: e.raw
                is Entry.PlaylistId -> e.playlistId
                is Entry.VideoUrl -> e.videoId
                is Entry.Unknown -> e.raw
            }.lowercase()
            if (key.isEmpty() || key in seen) duplicates.add(e) else { fresh.add(e); seen.add(key) }
        }
        return duplicates to fresh
    }
}

/** History pruning — same rule as shared/logic.js: 30 days kept, 31 pruned. */
object HistoryPruner {
    fun prune(entries: List<Long>, days: Int = 30, nowMillis: Long = System.currentTimeMillis()): List<Long> {
        val cutoff = days * 24L * 60 * 60 * 1000 + 1000 // 1s grace, as in JS
        return entries.filter { nowMillis - it <= cutoff }
    }
    fun isWithinDays(firstSeenMillis: Long, days: Int = 30, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis - firstSeenMillis <= days * 24L * 60 * 60 * 1000 + 1000
}

/** Download state machine — same 7 states and transitions as shared/logic.js. */
object DownloadStateMachine {
    enum class Event { Queue, Start, Pause, Resume, NetworkLost, NetworkBack, Retry, Fail, Complete }
    private val transitions: Map<String, Map<Event, String>> = mapOf(
        "Queued" to mapOf(Event.Start to "Downloading", Event.Pause to "PausedByUser", Event.Fail to "Failed"),
        "Downloading" to mapOf(Event.Pause to "PausedByUser", Event.NetworkLost to "WaitingForNetwork", Event.Fail to "Failed", Event.Complete to "Completed"),
        "WaitingForNetwork" to mapOf(Event.NetworkBack to "Downloading", Event.Pause to "PausedByUser", Event.Fail to "Failed", Event.Retry to "Retrying"),
        "Retrying" to mapOf(Event.Start to "Downloading", Event.Pause to "PausedByUser", Event.NetworkLost to "WaitingForNetwork", Event.Fail to "Failed", Event.Complete to "Completed"),
        "PausedByUser" to mapOf(Event.Resume to "Downloading", Event.Queue to "Queued", Event.Fail to "Failed"),
        "Completed" to emptyMap(),
        "Failed" to mapOf(Event.Retry to "Retrying", Event.Queue to "Queued"),
    )
    fun nextState(current: String, event: Event): String? = transitions[current]?.get(event)
    fun canTransition(current: String, event: Event): Boolean = nextState(current, event) != null
    const val MAX_RETRIES = 3
    fun maxRetriesReached(retryCount: Int, maxRetries: Int = MAX_RETRIES): Boolean = retryCount >= maxRetries
}
