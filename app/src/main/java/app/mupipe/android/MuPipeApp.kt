package app.mupipe.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import app.mupipe.android.data.AppContainer
import java.io.File

/**
 * Application class. Holds one AppContainer; no static singletons with state.
 * Startup is fail-soft: if the container cannot be built (e.g. a system
 * service throws on a device), the error is kept and MainActivity shows it
 * on screen instead of the app dying with no message. Any later crash is
 * also saved to mupipe-crash.txt so it can be reported.
 */
class MuPipeApp : Application() {
    var container: AppContainer? = null
        private set
    var containerError: Throwable? = null
        private set

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            runCatching {
                File(getExternalFilesDir(null), "mupipe-crash.txt")
                    .writeText(e.stackTraceToString())
            }
        }
        runCatching {
            container = AppContainer(this)
        }.onFailure { containerError = it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val channel = NotificationChannel(
                    "downloads", "Downloads", NotificationManager.IMPORTANCE_LOW
                ).apply { description = "MuPipe download progress" }
                getSystemService(NotificationManager::class.java)
                    ?.createNotificationChannel(channel)
            }
        }
    }
}
