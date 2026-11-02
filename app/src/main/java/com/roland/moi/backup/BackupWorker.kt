package com.roland.moi.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.roland.moi.data.Store
import java.util.concurrent.TimeUnit

/** Sauvegarde automatique : 1 minute après chaque changement, et toutes les 12 h. */
class BackupWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val c = applicationContext
        if (!DriveBackup.isConnected(c)) return Result.success()
        return when (val auth = DriveBackup.authorize(c)) {
            is DriveBackup.Auth.Token -> {
                val err = DriveBackup.upload(c, auth.token, Store.exportJson())
                if (err == null) Result.success() else Result.retry()
            }
            is DriveBackup.Auth.NeedsConsent -> Result.success() // l'utilisateur devra se reconnecter dans l'app
            is DriveBackup.Auth.Error -> Result.retry()
        }
    }

    companion object {
        private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun scheduleSoon(c: Context) {
            if (!DriveBackup.isConnected(c)) return
            val req = OneTimeWorkRequestBuilder<BackupWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .setConstraints(net)
                .build()
            WorkManager.getInstance(c).enqueueUniqueWork("moi_backup_soon", ExistingWorkPolicy.REPLACE, req)
        }

        fun schedulePeriodic(c: Context) {
            val req = PeriodicWorkRequestBuilder<BackupWorker>(12, TimeUnit.HOURS)
                .setConstraints(net)
                .build()
            WorkManager.getInstance(c).enqueueUniquePeriodicWork("moi_backup_periodic", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
