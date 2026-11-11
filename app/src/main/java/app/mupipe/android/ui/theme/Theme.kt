package app.mupipe.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Dark-only theme. No light theme exists in MuPipe. */
private val MuPipeColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    secondary = Color(0xFF81C995),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    error = Color(0xFFF28B82),
)

@Composable
fun MuPipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MuPipeColors, content = content)
}
