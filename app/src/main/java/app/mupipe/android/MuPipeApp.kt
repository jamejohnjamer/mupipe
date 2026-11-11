package app.mupipe.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import app.mupipe.android.data.AppContainer

/** Application class. Holds one AppContainer; no static singletons with state. */
class MuPipeApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "downloads", "Downloads", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "MuPipe download progress" }
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
