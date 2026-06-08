package no.mwmai.backtest.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import no.mwmai.backtest.core.Notifications
import no.mwmai.backtest.data.repo.PlatformRepository

/**
 * Polls the (public) job endpoint until the backtest reaches a terminal state,
 * then posts a local notification. Runs independently of the UI so the result
 * arrives even if the app is backgrounded.
 */
class BacktestPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val repo = PlatformRepository()

    override suspend fun doWork(): Result {
        val jobId = inputData.getInt(KEY_JOB_ID, -1)
        val label = inputData.getString(KEY_LABEL) ?: "backtest"
        if (jobId < 0) return Result.failure()

        repeat(MAX_POLLS) {
            val job = repo.job(jobId).getOrNull()
            if (job != null && job.isTerminal) {
                val (title, text) = when {
                    job.isSuccess -> "Backtest done" to "$label finished. Tap to view results."
                    job.status == "failed" ->
                        "Backtest failed" to "$label: ${job.error ?: "unknown error"}"
                    else -> "Backtest ${job.status}" to label
                }
                Notifications.notifyJobDone(applicationContext, jobId, title, text)
                return Result.success()
            }
            delay(POLL_INTERVAL_MS)
        }
        return Result.success() // gave up quietly; the in-app screen keeps polling
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_LABEL = "label"
        private const val MAX_POLLS = 60
        private const val POLL_INTERVAL_MS = 10_000L
    }
}
