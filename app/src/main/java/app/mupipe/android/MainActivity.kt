package app.mupipe.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.mupipe.android.ui.screens.DownloadsScreen
import app.mupipe.android.ui.screens.GroupsScreen
import app.mupipe.android.ui.screens.HistoryScreen
import app.mupipe.android.ui.screens.HomeScreen
import app.mupipe.android.ui.screens.PlaylistsScreen
import app.mupipe.android.ui.screens.SearchScreen
import app.mupipe.android.ui.screens.SettingsScreen
import app.mupipe.android.ui.screens.SubscriptionsScreen
import app.mupipe.android.ui.theme.MuPipeTheme
import kotlinx.coroutines.launch

/**
 * Single activity. Saved state holds only small IDs (route strings),
 * never video lists or bundles — this avoids the PipePipe #652
 * TransactionTooLargeException (769,428-byte saved state).
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MuPipeApp).container
        setContent {
            MuPipeTheme {
                val nav = rememberNavController()
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                var current by remember { mutableStateOf("home") }
                val items = listOf(
                    "home" to "Home", "search" to "Search", "subscriptions" to "Subscriptions",
                    "groups" to "Subscription Groups", "history" to "History",
                    "playlists" to "Playlists", "downloads" to "Downloads", "settings" to "Settings",
                )
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Text("MuPipe", modifier = Modifier.padding(16.dp))
                            items.forEach { (route, label) ->
                                NavigationDrawerItem(
                                    label = { Text(label) },
                                    selected = current == route,
                                    onClick = {
                                        current = route
                                        scope.launch { drawerState.close() }
                                        nav.navigate(route) { launchSingleTop = true }
                                    },
                                )
                            }
                        }
                    },
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(items.first { it.first == current }.second) },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
                            composable("home") { HomeScreen(container) {} }
                            composable("search") { SearchScreen(container) {} }
                            composable("subscriptions") { SubscriptionsScreen(container) }
                            composable("groups") { GroupsScreen(container) }
                            composable("history") { HistoryScreen(container) }
                            composable("playlists") { PlaylistsScreen(container) }
                            composable("downloads") { DownloadsScreen(container) }
                            composable("settings") { SettingsScreen(container) }
                        }
                    }
                }
            }
        }
    }
}
