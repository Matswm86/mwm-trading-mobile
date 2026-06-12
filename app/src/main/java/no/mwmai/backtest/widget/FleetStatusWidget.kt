package no.mwmai.backtest.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Home-screen fleet heartbeat: one row per account group (Funded / Funded
 * save / Practice) with an OK-count and a worst-status dot, plus market state
 * and feed freshness. Display-only and free to run — alerting is ntfy's job.
 */
class FleetStatusWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = FleetStatusStore.snapshot(context)
        provideContent { WidgetContent(snap) }
    }
}

class FleetStatusWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FleetStatusWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FleetStatusWorker.schedule(context)
        FleetStatusWorker.refreshNow(context) // don't sit on "tap to load" for 15 min
    }

    // onUpdate also fires on the updatePeriodMillis backstop and after reboot;
    // re-enqueueing unique periodic work here is idempotent and self-heals.
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        FleetStatusWorker.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        FleetStatusWorker.cancel(context)
    }
}

class RefreshFleetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        FleetStatusWorker.refreshNow(context)
    }
}

// Deep-ink palette, mirrored from ui/theme/Color.kt (Glance can't read the
// Compose theme objects directly).
private val BgColor = ColorProvider(Color(0xFF0D1422))
private val TextMain = ColorProvider(Color(0xFFEAF0F8))
private val TextMuted = ColorProvider(Color(0xFF7C8AA5))
private val Ok = Color(0xFF2DD4A7)
private val Warn = Color(0xFFFBBF24)
private val Down = Color(0xFFFB7185)
private val Idle = Color(0xFF7C8AA5)

private fun statusColor(status: String): Color = when (status) {
    "OK", "IDLE" -> Ok
    "SETTLING" -> Idle
    "WARN", "STALE" -> Warn
    "DOWN" -> Down
    else -> Idle
}

private fun marketLabel(state: String): String = when (state) {
    "OPEN" -> "market open"
    "SETTLING" -> "market open · settling"
    "WEEKEND" -> "weekend"
    "DAILY_BREAK" -> "daily break"
    "HOLIDAY" -> "holiday"
    "HOLIDAY_EARLY_CLOSE" -> "holiday close"
    else -> state.lowercase()
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
private fun WidgetContent(snap: FleetStatusStore.Snapshot) {
    val dto = snap.status
    val now = System.currentTimeMillis()
    val serverStale = dto?.generatedAt
        ?.let { now - it.toEpochMilli() > FleetStatusStore.SERVER_STALE_MS } ?: true
    val effectiveOverall = when {
        dto == null -> "?"
        serverStale -> "STALE"
        else -> dto.overall
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(18.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .clickable(actionRunCallback<RefreshFleetAction>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(statusColor(effectiveOverall), 10.dp)
            Spacer(GlanceModifier.width(7.dp))
            Text(
                "FLEET",
                style = TextStyle(color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                dto?.market?.state?.let(::marketLabel) ?: "no data",
                style = TextStyle(color = TextMuted, fontSize = 11.sp),
            )
        }
        Spacer(GlanceModifier.height(8.dp))

        if (dto == null) {
            Text(
                "Tap to load fleet status",
                style = TextStyle(color = TextMuted, fontSize = 12.sp),
            )
        } else {
            dto.groups.forEach { g ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(statusColor(if (serverStale) "STALE" else g.status), 8.dp)
                    Spacer(GlanceModifier.width(7.dp))
                    Text(
                        g.label,
                        style = TextStyle(color = TextMain, fontSize = 12.sp),
                    )
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        "${g.ok}/${g.total}",
                        style = TextStyle(
                            color = if (g.ok == g.total) TextMuted else ColorProvider(Warn),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        Spacer(GlanceModifier.defaultWeight())
        val footer = buildString {
            if (snap.fetchedAtMs > 0) {
                append("upd ").append(timeFmt.format(Instant.ofEpochMilli(snap.fetchedAtMs)))
            }
            if (!snap.fetchOk) append(" · offline")
            if (dto != null && serverStale && snap.fetchOk) append(" · FEED STALE")
        }
        Text(footer.ifEmpty { "—" }, style = TextStyle(color = TextMuted, fontSize = 10.sp))
    }
}

@Composable
private fun Dot(color: Color, sizeDp: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = GlanceModifier
            .size(sizeDp)
            .background(ColorProvider(color))
            .cornerRadius(sizeDp / 2),
    ) {}
}
