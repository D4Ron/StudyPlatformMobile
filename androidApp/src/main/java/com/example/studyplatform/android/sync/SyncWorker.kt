package com.example.studyplatform.android.sync

import android.content.Context
import androidx.work.*
import com.example.studyplatform.data.AppData
import java.util.concurrent.TimeUnit

/**
 * Reconciles the device with the server in the background.
 *
 * Constrained to run only when a connection exists, so nothing is attempted — and no
 * data spent — on a phone that is offline. WorkManager holds the request across reboots
 * and process death, which is what makes "written on the bus, uploaded that evening"
 * work without the user opening the app again.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val outcome = AppData.syncEngine.sync()

        // Retry rather than fail: the queue is durable, so a failed attempt costs
        // nothing but a later one. WorkManager backs off on its own.
        return if (outcome.failed) Result.retry() else Result.success()
    }

    companion object {
        private const val PERIODIC = "sync-periodic"
        private const val ONE_OFF = "sync-now"

        private val networked = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Schedules the recurring sync. Called once, at startup.
         *
         * Fifteen minutes is WorkManager's floor for periodic work and is the right
         * order of magnitude anyway: notes are not collaborative in real time, and a
         * tighter loop would spend a student's data on nothing.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(networked)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC,
                // KEEP, so a restart does not reset the interval and cause a burst of
                // syncs every time the app is opened.
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Pushes now — after a write, or when the user pulls to refresh. */
        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(networked)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_OFF,
                // A second request while one is queued replaces it: they would do the
                // same work, and the queue is drained whole either way.
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
