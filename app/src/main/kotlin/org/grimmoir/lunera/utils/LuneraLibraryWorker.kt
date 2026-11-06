package org.grimmoir.lunera.utils
import android.content.Context
import androidx.work.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
@EntryPoint @InstallIn(SingletonComponent::class)
interface LuneraLibraryAccess { fun database(): org.grimmoir.lunera.db.MusicDatabase }
class LuneraLibraryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_AUDIO else android.Manifest.permission.READ_EXTERNAL_STORAGE
        if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) return Result.success()
        return try {
            val db = EntryPointAccessors.fromApplication(applicationContext, LuneraLibraryAccess::class.java).database()
            LocalAudioScanner.scanAndInsert(applicationContext, db)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { Result.retry() }
    }
    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LuneraLibraryWorker>(6, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("lunera-library-refresh", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
