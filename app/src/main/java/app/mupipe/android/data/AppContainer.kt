package app.mupipe.android.data

import android.content.Context
import app.mupipe.android.data.db.MuPipeDatabase
import app.mupipe.android.data.provider.InvidiousApi
import app.mupipe.android.data.provider.InvidiousInstances
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** One container, built once in MuPipeApp. IO work stays on Dispatchers.IO. */
class AppContainer(context: Context) {
    val db: MuPipeDatabase = MuPipeDatabase.create(context.applicationContext)
    val ioDispatcher = Dispatchers.IO

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    var invidiousBaseUrl: String = InvidiousInstances.DEFAULTS.first()
        private set

    fun invidiousApi(baseUrl: String = invidiousBaseUrl): InvidiousApi =
        Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(InvidiousApi::class.java)

    /** Cheap health check: try instances in order, keep the first that answers. */
    suspend fun pickHealthyInstance(): String {
        for (base in InvidiousInstances.DEFAULTS) {
            val ok = runCatching {
                invidiousApi(base).suggestions("a")
                true
            }.getOrDefault(false)
            if (ok) { invidiousBaseUrl = base; return base }
        }
        return invidiousBaseUrl
    }

    val repository = Repository(this)
    val downloadManager = DownloadManager(context.applicationContext, db)
}
