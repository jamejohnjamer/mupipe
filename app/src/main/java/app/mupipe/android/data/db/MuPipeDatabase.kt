package app.mupipe.android.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database v1. Migrations are recorded here; v1 -> v2 is a stub
 * placeholder that must be filled before versionCode is raised.
 * Never store large objects in saved-state Bundles — only IDs.
 */
@Database(
    entities = [
        GroupEntity::class, SubscriptionEntity::class, UploadHistoryEntity::class,
        PlaylistEntity::class, PlaylistEntryEntity::class, DownloadEntity::class,
        SearchHistoryEntity::class, PreferenceEntity::class,
    ],
    version = 1, exportSchema = false
)
abstract class MuPipeDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun uploadHistoryDao(): UploadHistoryDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v2 plan (not yet shipped): add `thumbnailUrl` to subscriptions.
                // db.execSQL("ALTER TABLE subscriptions ADD COLUMN thumbnailUrl TEXT")
            }
        }

        fun create(context: Context): MuPipeDatabase =
            Room.databaseBuilder(context, MuPipeDatabase::class.java, "mupipe.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
