package no.mwmai.backtest.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import no.mwmai.backtest.MainActivity
import no.mwmai.backtest.R

object Notifications {
    const val CHANNEL_ID = "backtest_jobs"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Backtest jobs",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Fires when a queued backtest finishes." }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /** Post a completion notification. No-op if POST_NOTIFICATIONS is denied. */
    fun notifyJobDone(context: Context, notifId: Int, title: String, text: String) {
        ensureChannel(context)
        val tap = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notifId, n)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted; the in-app poll still shows the result.
        }
    }
}
