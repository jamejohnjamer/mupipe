package app.mupipe.android.data.provider

import app.mupipe.android.data.model.SearchFilters

/**
 * Maps MuPipe filters to Invidious query params. Every mapped key is
 * server-side. Unknown values throw — never passed to the server.
 * Mirrors shared/logic.js mapSearchFiltersToInvidious exactly.
 */
object SearchFilterMapper {
    fun toParams(filters: SearchFilters): Map<String, String> {
        val params = mutableMapOf<String, String>()
        val typeMap = mapOf("video" to "video", "channel" to "channel", "playlist" to "playlist", "movie" to "movie", "show" to "show", "all" to "all")
        params["type"] = typeMap[filters.type] ?: throw IllegalArgumentException("Unknown type value: ${filters.type}")
        val sortMap = mapOf("relevance" to "relevance", "upload_date" to "upload_date", "view_count" to "view_count")
        params["sort_by"] = sortMap[filters.sort] ?: throw IllegalArgumentException("Unknown sort value: ${filters.sort}")
        filters.date?.let {
            val m = mapOf("hour" to "hour", "today" to "today", "week" to "week", "month" to "month", "year" to "year")
            params["date"] = m[it] ?: throw IllegalArgumentException("Unknown date value: $it")
        }
        filters.duration?.let {
            val m = mapOf("short" to "short", "medium" to "medium", "long" to "long")
            params["duration"] = m[it] ?: throw IllegalArgumentException("Unknown duration value: $it")
        }
        val features = buildList {
            if (filters.liveOnly) add("live")
            if (filters.subtitlesOnly) add("subtitles")
        }
        if (features.isNotEmpty()) params["features"] = features.joinToString(",")
        return params
    }
}
