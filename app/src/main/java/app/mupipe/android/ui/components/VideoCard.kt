package app.mupipe.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.mupipe.android.data.model.VideoItem
import coil.compose.AsyncImage

/** One video card. Tap = details (never playback — MuPipe has no player). */
@Composable
fun VideoCard(
    video: VideoItem,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onClick: (VideoItem) -> Unit,
    onToggleSelect: ((VideoItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { if (selectionMode) onToggleSelect?.invoke(video) else onClick(video) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) Checkbox(checked = selected, onCheckedChange = { onToggleSelect?.invoke(video) })
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(120.dp, 68.dp).clip(RoundedCornerShape(6.dp)),
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(video.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Text(
                    listOfNotNull(video.channelName, formatDuration(video.durationSeconds),
                        video.viewCount?.let { formatViews(it) }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                if (video.isLive) Text("LIVE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

fun formatDuration(seconds: Long?): String {
    if (seconds == null) return ""
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatViews(n: Long): String = when {
    n >= 1_000_000 -> "%.1fM views".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK views".format(n / 1_000.0)
    else -> "$n views"
}
