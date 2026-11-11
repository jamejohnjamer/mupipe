package app.mupipe.android.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.mupipe.android.MuPipeApp
import app.mupipe.android.data.db.DownloadEntity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

/**
 * One download per worker. Plain HTTPS range requests only.
 * Resume only when the server answers 206 / Accept-Ranges.
 * Max 3 retries (WorkManager backoff); after that the job is Failed,
 * never an unlimited retry loop. No DRM, no expired-URL tricks:
 * if the provider URL is gone (4xx), fail honestly.
 */
class DownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        const val KEY_VIDEO_ID = "videoId"
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "fileName"
        const val MAX_RETRIES = 3
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    override suspend fun doWork(): Result {
        val videoId = inputData.getString(KEY_VIDEO_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "$videoId.bin"
        val app = applicationContext as MuPipeApp
        val dao = app.container.db.downloadDao()
        val existing = dao.byId(videoId) ?: return Result.failure()
        if (existing.state == "PausedByUser") return Result.success() // never resume a paused job
        if (existing.retryCount >= MAX_RETRIES) {
            dao.update(existing.copy(state = "Failed", updatedAtMillis = System.currentTimeMillis()))
            return Result.failure()
        }
        val outDir = File(applicationContext.getExternalFilesDir(null), "MuPipe")
        if (!outDir.exists()) outDir.mkdirs()
        val outFile = File(outDir, fileName)
        val downloaded = if (outFile.exists()) outFile.length() else 0L
        dao.update(existing.copy(state = "Downloading", bytesDownloaded = downloaded, updatedAtMillis = System.currentTimeMillis()))
        return try {
            val reqBuilder = Request.Builder().url(url)
            if (downloaded > 0) reqBuilder.header("Range", "bytes=$downloaded-")
            val resp = http.newCall(reqBuilder.build()).execute()
            resp.use { r ->
                when {
                    r.code == 416 -> { // already complete
                        dao.update(existing.copy(state = "Completed", updatedAtMillis = System.currentTimeMillis()))
                        return Result.success()
                    }
                    !r.isSuccessful -> {
                        // 4xx = provider refused (expired / not authorised): fail, do not retry forever.
                        val next = existing.copy(state = "Failed", retryCount = existing.retryCount + 1, updatedAtMillis = System.currentTimeMillis())
                        dao.update(next)
                        return if (r.code in 400..499) Result.failure() else Result.retry()
                    }
                    else -> {
                        val body = r.body ?: return Result.failure()
                        val total = (r.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                            ?: r.header("Content-Length")?.toLongOrNull()?.plus(downloaded) ?: -1L)
                        val append = r.code == 206
                        RandomAccessFile(outFile, "rw").use { raf ->
                            if (append) raf.seek(downloaded) else raf.setLength(0)
                            val buf = ByteArray(64 * 1024)
                            var read: Int
                            var done = if (append) downloaded else 0L
                            val stream = body.byteStream()
                            var lastReport = System.currentTimeMillis()
                            while (stream.read(buf).also { read = it } != -1) {
                                if (isStopped) {
                                    dao.update(existing.copy(state = "PausedByUser", bytesDownloaded = done, updatedAtMillis = System.currentTimeMillis()))
                                    return Result.success()
                                }
                                raf.write(buf, 0, read)
                                done += read
                                if (System.currentTimeMillis() - lastReport > 1000) {
                                    lastReport = System.currentTimeMillis()
                                    dao.update(existing.copy(state = "Downloading", bytesDownloaded = done, totalBytes = total, updatedAtMillis = System.currentTimeMillis()))
                                }
                            }
                            dao.update(existing.copy(state = "Completed", bytesDownloaded = done, totalBytes = if (total > 0) total else done, updatedAtMillis = System.currentTimeMillis()))
                        }
                        return Result.success()
                    }
                }
            }
        } catch (e: Exception) {
            // Network loss / timeout: WaitingForNetwork, WorkManager retries with backoff.
            val next = existing.copy(
                state = "WaitingForNetwork", retryCount = existing.retryCount + 1,
                updatedAtMillis = System.currentTimeMillis(),
            )
            dao.update(next)
            if (next.retryCount >= MAX_RETRIES) {
                dao.update(next.copy(state = "Failed"))
                Result.failure()
            } else Result.retry()
        }
    }
}
