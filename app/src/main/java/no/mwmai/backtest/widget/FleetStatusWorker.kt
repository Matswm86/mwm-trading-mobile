package no.mwmai.backtest.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background refresh for the fleet widget: fetch the ~2KB status JSON, then
 * redraw every widget instance. Periodic at WorkManager's 15-minute floor —
 * instant alerting is ntfy's job, the widget is the glanceable state.
 */
class FleetStatusWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        FleetStatusStore.refresh(applicationContext)
        FleetStatusWidget().updateAll(applicationContext)
        Result.success() // failures keep the last snapshot; widget shows it as offline
    }

    companion object {
        private const val PERIODIC_WORK = "fleet_status_refresh"
        private const val ONESHOT_WORK = "fleet_status_refresh_now"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FleetStatusWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun refreshNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONESHOT_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<FleetStatusWorker>().build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
