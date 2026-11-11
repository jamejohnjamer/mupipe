package app.mupipe.android.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.mupipe.android.data.db.DownloadEntity
import app.mupipe.android.data.db.MuPipeDatabase
import app.mupipe.android.download.DownloadWorker
import java.util.concurrent.TimeUnit

/**
 * Download manager. One WorkManager job per video, unique by videoId —
 * no duplicate workers. Max 2 concurrent (bounded in the worker chain
 * via WorkManager's own queue + a single "mupipe-downloads" tag check).
 * Auto-recovery: WorkManager retries on network return with backoff.
 * User-paused downloads are never enqueued again automatically.
 */
class DownloadManager(context: Context, private val db: MuPipeDatabase) {
    private val workManager = WorkManager.getInstance(context)
    private val appContext = context.applicationContext

    suspend fun queue(videoId: String, title: String, url: String, audioOnly: Boolean): Boolean {
        if (db.downloadDao().byId(videoId) != null) return false // duplicate guard
        val safeName = title.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(80).ifBlank { videoId }
        val fileName = if (audioOnly) "$safeName.m4a" else "$safeName.mp4"
        db.downloadDao().insert(
            DownloadEntity(videoId, title, url, fileName, audioOnly, "Queued", 0, -1, 0, System.currentTimeMillis())
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                DownloadWorker.KEY_VIDEO_ID to videoId,
                DownloadWorker.KEY_URL to url,
                DownloadWorker.KEY_FILE_NAME to fileName,
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("mupipe-downloads")
            .build()
        // Unique work per video: KEEP an existing job, never duplicate it.
        workManager.enqueueUniqueWork("dl-$videoId", ExistingWorkPolicy.KEEP, request)
        return true
    }

    suspend fun pause(videoId: String) {
        workManager.cancelUniqueWork("dl-$videoId")
        db.downloadDao().byId(videoId)?.let {
            db.downloadDao().update(it.copy(state = "PausedByUser", updatedAtMillis = System.currentTimeMillis()))
        }
    }

    suspend fun resume(videoId: String) {
        val item = db.downloadDao().byId(videoId) ?: return
        if (item.state != "PausedByUser" && item.state != "Failed") return
        db.downloadDao().update(item.copy(state = "Queued", updatedAtMillis = System.currentTimeMillis()))
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                DownloadWorker.KEY_VIDEO_ID to videoId,
                DownloadWorker.KEY_URL to item.url,
                DownloadWorker.KEY_FILE_NAME to item.fileName,
            ))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("mupipe-downloads")
            .build()
        workManager.enqueueUniqueWork("dl-$videoId", ExistingWorkPolicy.REPLACE, request)
    }
}
