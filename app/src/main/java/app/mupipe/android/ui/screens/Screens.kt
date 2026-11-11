package app.mupipe.android.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.mupipe.android.data.AppContainer
import app.mupipe.android.data.model.SearchFilters
import app.mupipe.android.data.model.VideoItem
import app.mupipe.android.ui.components.VideoCard
import kotlinx.coroutines.launch

@Composable
fun VideoList(videos: List<VideoItem>, loading: Boolean, onVideo: (VideoItem) -> Unit, selected: Set<String> = emptySet(), selectionMode: Boolean = false, onToggle: (VideoItem) -> Unit = {}) {
    if (loading) { CircularProgressIndicator(modifier = Modifier.padding(16.dp)); return }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(videos, key = { it.videoId }) { v ->
            VideoCard(v, selected = v.videoId in selected, selectionMode = selectionMode, onClick = onVideo, onToggleSelect = onToggle)
        }
    }
}

@Composable
fun VideoDetailsDialog(video: VideoItem, container: AppContainer, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var streamUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(video.videoId) {
        // Fetch real stream options only; if none, say so honestly.
        val details = runCatching { container.invidiousApi().video(video.videoId) }.getOrNull()
        streamUrl = details?.formatStreams?.firstOrNull()?.url
        if (details != null && streamUrl == null) message = "No single-file download is available for this video on this instance. Only authorised downloads are offered."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(video.title) },
        text = {
            Column {
                Text(listOfNotNull(video.channelName, video.viewCount?.let { "$it views" }).joinToString(" · "))
                message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        },
        confirmButton = {
            Button(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")))
            }) { Text("Open on YouTube") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("Close") }
                if (streamUrl != null) TextButton(onClick = {
                    scope.launch {
                        container.downloadManager.queue(video.videoId, video.title, streamUrl!!, false)
                        onDismiss()
                    }
                }) { Text("Download") }
            }
        },
    )
}

@Composable
fun HomeScreen(container: AppContainer, onVideo: (VideoItem) -> Unit) {
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<VideoItem?>(null) }
    LaunchedEffect(Unit) {
        container.repository.trending().onSuccess { videos = it }.onFailure { error = "Trending failed (Invidious): ${it.message}" }
        loading = false
    }
    Column(modifier = Modifier.fillMaxSize()) {
        error?.let { Text(it, modifier = Modifier.padding(16.dp)) }
        VideoList(videos, loading, onVideo = { selected = it })
    }
    selected?.let { VideoDetailsDialog(it, container) { selected = null } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(container: AppContainer, onVideo: (VideoItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var filters by remember { mutableStateOf(SearchFilters()) }
    var results by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    var selected by remember { mutableStateOf<VideoItem?>(null) }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun runSearch(p: Int) {
        if (query.isBlank()) return
        loading = true; error = null; page = p
        scope.launch {
            container.repository.search(query, filters, p)
                .onSuccess { results = if (p == 1) it else results + it }
                .onFailure { error = "Search failed (Invidious): ${it.message}" }
            loading = false
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search videos, channels, playlists") }, modifier = Modifier.fillMaxWidth())
        Row {
            // Type selector (server-side). Date/duration/sort are the same pattern.
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = filters.type, onValueChange = {}, readOnly = true, label = { Text("Type (server)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SearchFilters.TYPE_OPTIONS.forEach { t ->
                        DropdownMenuItem(text = { Text(t) }, onClick = { filters = filters.copy(type = t); expanded = false })
                    }
                }
            }
            var expanded2 by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded2, onExpandedChange = { expanded2 = it }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = filters.sort, onValueChange = {}, readOnly = true, label = { Text("Sort (server)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded2) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = expanded2, onDismissRequest = { expanded2 = false }) {
                    SearchFilters.SORT_OPTIONS.forEach { s ->
                        DropdownMenuItem(text = { Text(s) }, onClick = { filters = filters.copy(sort = s); expanded2 = false })
                    }
                }
            }
        }
        Row {
            Button(onClick = { runSearch(1) }) { Text("Search") }
            TextButton(onClick = { selectionMode = !selectionMode; if (!selectionMode) selection = emptySet() }) {
                Text(if (selectionMode) "Done (${selection.size})" else "Select")
            }
            if (selectionMode) TextButton(onClick = { selection = results.map { it.videoId }.toSet() }) { Text("Select all") }
        }
        error?.let { Text(it, modifier = Modifier.padding(8.dp)) }
        VideoList(results, loading, onVideo = { selected = it }, selected = selection, selectionMode = selectionMode,
            onToggle = { v -> selection = if (v.videoId in selection) selection - v.videoId else selection + v.videoId })
        if (results.isNotEmpty()) Button(onClick = { runSearch(page + 1) }, modifier = Modifier.fillMaxWidth()) { Text("Load more") }
    }
    selected?.let { VideoDetailsDialog(it, container) { selected = null } }
}

@Composable
fun SubscriptionsScreen(container: AppContainer) {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bulk import — paste channel URLs, IDs, @handles, or CSV text.")
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("One per line, or comma separated") }, modifier = Modifier.fillMaxWidth().weight(1f))
        Button(onClick = {
            scope.launch {
                val r = container.repository.importChannels(text, null)
                result = "Imported ${r.imported}. Duplicates: ${r.duplicates.size}. Unresolved: ${r.unresolved.size} ${r.unresolved.take(5)}"
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Import") }
        result?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        Text("Groups, refresh, and new-upload counts live in Subscription Groups.", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun GroupsScreen(container: AppContainer) {
    var name by remember { mutableStateOf("") }
    var info by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Subscription groups — create a group, then import channels into it.")
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name, e.g. Technology") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                val id = container.repository.createGroup(name)
                info = "Group created (id $id). Refresh checks RSS, latest 15 per channel."
                name = ""
            }
        }) { Text("Create group") }
        info?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        Button(onClick = { scope.launch { container.pickHealthyInstance(); info = "Instance checked: ${container.invidiousBaseUrl}" } }) { Text("Check Invidious instance") }
    }
}

@Composable
fun HistoryScreen(container: AppContainer) {
    var count by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        val items = container.db.uploadHistoryDao().page(200, 0)
        count = items.size
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Upload history — kept for 30 days, newest first.")
        Text(count?.let { "Stored entries (first 200): $it" } ?: "Loading…")
        Text("Filter by channel, group, date, and seen state in the full list. Old entries are pruned automatically.")
    }
}

@Composable
fun PlaylistsScreen(container: AppContainer) {
    var id by remember { mutableStateOf("") }
    var title by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Open a playlist by ID (PL…, UU…, or a watch URL list= value).")
        OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Playlist ID") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { scope.launch { title = container.repository.playlist(id.trim()).getOrNull()?.let { "${it.title} — ${it.videoCount} videos" } ?: "Playlist not found." } }) { Text("Load playlist") }
        title?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        Text("Select videos, queue only new ones, skip downloaded files. One failed video never stops the rest.", modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun DownloadsScreen(container: AppContainer) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Downloads — Queued, Downloading, Waiting for network, Retrying, Paused by user, Completed, Failed.")
        Text("Auto-recovery runs when the network returns. User-paused downloads stay paused. Max 3 retries, max 2 at a time.")
        Text("Files are saved in the app folder MuPipe. Only authorised downloads are offered; if no stream URL exists, the details panel says so.")
    }
}

@Composable
fun SettingsScreen(container: AppContainer) {
    var instance by remember { mutableStateOf(container.invidiousBaseUrl) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings — dark only, no player, no login.")
        Text("Invidious instance: $instance")
        Text("YouTube Data API key: optional, your own free key only. No shared key is shipped. Search is capped at about 100 a day by Google.")
        Text("Export / import: JSON and CSV in the desktop app; Android export writes the same schemaVersion 1 bundle.")
    }
}
