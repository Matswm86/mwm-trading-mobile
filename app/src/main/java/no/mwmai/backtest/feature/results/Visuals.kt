package no.mwmai.backtest.feature.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import no.mwmai.backtest.data.model.RunDto
import no.mwmai.backtest.data.model.RunStats
import no.mwmai.backtest.data.model.TradeDto
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.OutlineSoft
import no.mwmai.backtest.ui.theme.Positive
import no.mwmai.backtest.ui.theme.Warn
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

// ─── shared section chrome ─────────────────────────────────────────────────

@Composable
fun ChartSection(
    title: String,
    caption: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSoft, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted)
        Box(modifier = Modifier.padding(top = 10.dp)) { content() }
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

// ─── plain-language verdict ────────────────────────────────────────────────

/** Says what the numbers mean in plain English. Uses the deflated (luck-
 *  adjusted) figures for the robustness line, never the naive Sharpe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerdictCard(run: RunDto, stats: RunStats?) {
    val net = run.netPnl
    val profitable = (net ?: 0.0) >= 0.0
    val std = stats?.standard?.takeIf { it.available }
    val adv = stats?.advanced?.takeIf { it.available }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                (if (profitable) Positive else Negative).copy(alpha = 0.35f),
                RoundedCornerShape(18.dp),
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(
            if (profitable) "PROFITABLE" else "LOSING",
            style = MaterialTheme.typography.titleLarge,
            color = if (profitable) Positive else Negative,
        )
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            net?.let {
                PlainLine(
                    "${if (it >= 0) "Made" else "Lost"} ${moneyAbs(it)} over " +
                        "${run.nTrades} trades.",
                )
            }
            std?.profitFactor?.let {
                PlainLine("Won $${"%.2f".format(it)} for every $1 it lost.")
            }
            std?.maxDrawdown?.let {
                PlainLine("At its worst it was ${moneyAbs(-abs(it))} below its best point.")
            }
            if (std?.winRate != null && std.avgWin != null && std.avgLoss != null) {
                PlainLine(
                    "Won ${pctFmt(std.winRate!!)} of trades — typical win ${moneyAbs(std.avgWin!!)}, " +
                        "typical loss ${moneyAbs(std.avgLoss!!)}.",
                )
            }
            when (adv?.dsrPass) {
                true -> PlainLine(
                    "Luck check: PASSED — still looks like real edge after adjusting for chance.",
                    Positive,
                )
                false -> PlainLine(
                    "Luck check: FAILED — after adjusting for chance, this could be random.",
                    Warn,
                )
                null -> if (stats == null) {
                    PlainLine("Running the luck-adjustment checks…", Muted)
                }
            }
        }
    }
}

@Composable
private fun PlainLine(text: String, color: Color? = null) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = color ?: MaterialTheme.colorScheme.onSurface,
    )
}

// ─── drawdown (underwater) chart ───────────────────────────────────────────

@Composable
fun DrawdownChart(curve: List<Double>, modifier: Modifier = Modifier) {
    if (curve.size < 2) return
    val dd = remember(curve) {
        var peak = 0.0
        curve.map { v ->
            if (v > peak) peak = v
            peak - v
        }
    }
    val maxDd = dd.max().takeIf { it > 0 } ?: return

    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            dd.forEachIndexed { i, v ->
                lineTo(w * i / (dd.size - 1), ((v / maxDd) * h).toFloat())
            }
            lineTo(w, 0f)
            close()
        }
        drawPath(
            path,
            brush = Brush.verticalGradient(
                listOf(Negative.copy(alpha = 0.10f), Negative.copy(alpha = 0.45f)),
            ),
        )
        drawLine(Muted.copy(alpha = 0.6f), Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1.5f)
    }
}

// ─── P&L histogram ─────────────────────────────────────────────────────────

@Composable
fun PnlHistogram(pnls: List<Double>, modifier: Modifier = Modifier) {
    if (pnls.size < 3) return
    val bins = 15
    val lo = pnls.min()
    val hi = pnls.max()
    val span = (hi - lo).takeIf { it > 0 } ?: return
    val counts = IntArray(bins)
    val centers = DoubleArray(bins)
    pnls.forEach { p ->
        val b = (((p - lo) / span) * bins).toInt().coerceIn(0, bins - 1)
        counts[b]++
    }
    for (b in 0 until bins) centers[b] = lo + (b + 0.5) * span / bins
    val maxCount = counts.max()

    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val w = size.width
        val h = size.height
        val bw = w / bins
        for (b in 0 until bins) {
            if (counts[b] == 0) continue
            val bh = (counts[b].toFloat() / maxCount) * (h - 4f)
            val color = if (centers[b] >= 0) Positive else Negative
            drawRoundRect(
                color = color.copy(alpha = 0.85f),
                topLeft = Offset(b * bw + 1.5f, h - bh),
                size = androidx.compose.ui.geometry.Size(bw - 3f, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
            )
        }
        // zero marker
        if (lo < 0 && hi > 0) {
            val x = ((0.0 - lo) / span * w).toFloat()
            drawLine(Muted, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5f)
        }
    }
}

// ─── win/loss anatomy ──────────────────────────────────────────────────────

@Composable
fun WinLossVisual(winRate: Double, avgWin: Double, avgLoss: Double) {
    val wr = (if (winRate <= 1.5) winRate else winRate / 100).coerceIn(0.0, 1.0)
    val maxAvg = max(abs(avgWin), abs(avgLoss)).takeIf { it > 0 } ?: return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // win-rate split bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(wr.toFloat().coerceAtLeast(0.01f))
                    .height(22.dp)
                    .background(
                        Positive.copy(alpha = 0.8f),
                        RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { BarLabel("${pctFmt(wr)} win") }
            Box(
                modifier = Modifier
                    .weight((1f - wr.toFloat()).coerceAtLeast(0.01f))
                    .height(22.dp)
                    .background(
                        Negative.copy(alpha = 0.65f),
                        RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) { BarLabel("${pctFmt(1 - wr)} loss") }
        }
        SizeBar("Typical win", abs(avgWin), maxAvg, Positive)
        SizeBar("Typical loss", abs(avgLoss), maxAvg, Negative)
    }
}

@Composable
private fun BarLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF04130C),
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
private fun SizeBar(label: String, value: Double, maxValue: Double, color: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
            modifier = Modifier.fillMaxWidth(0.28f),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / maxValue).toFloat().coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .background(color.copy(alpha = 0.85f), RoundedCornerShape(5.dp)),
            )
        }
        Text(
            moneyAbs(value),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ─── exit reasons ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExitReasonChips(trades: List<TradeDto>) {
    val counts = remember(trades) {
        trades.groupingBy { it.exitReason ?: "?" }.eachCount().entries.sortedByDescending { it.value }
    }
    if (counts.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        counts.forEach { (reason, n) ->
            val color = when (reason) {
                "TP", "TP1", "TP2", "TP3" -> Positive
                "SL" -> Negative
                else -> Muted
            }
            Text(
                "$reason × $n",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = color,
                modifier = Modifier
                    .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

// ─── 3D P&L terrain (weekday × time-of-day, ET) ────────────────────────────

private const val DAYS = 5
private const val BLOCKS = 12 // 2-hour blocks, 00–24 ET

private fun buildTerrain(trades: List<TradeDto>): Array<DoubleArray>? {
    val et = ZoneId.of("America/New_York")
    val grid = Array(DAYS) { DoubleArray(BLOCKS) }
    var hits = 0
    trades.forEach { t ->
        val ts = t.entryTs ?: return@forEach
        val pnl = t.pnlUsd ?: return@forEach
        val z = runCatching { OffsetDateTime.parse(ts).atZoneSameInstant(et) }.getOrNull()
            ?: return@forEach
        val day = z.dayOfWeek.value // 1=Mon..7=Sun
        if (day > DAYS) return@forEach
        grid[day - 1][z.hour / 2] += pnl
        hits++
    }
    return if (hits >= 3) grid else null
}

/** Pseudo-3D bar terrain on a plain Canvas (no GL dependency). Height = |P&L|
 *  in the bucket, color = direction. Horizontal drag rotates the scene. */
@Composable
fun Pnl3DTerrain(trades: List<TradeDto>, modifier: Modifier = Modifier) {
    val grid = remember(trades) { buildTerrain(trades) } ?: return
    val maxAbs = grid.maxOf { row -> row.maxOf { abs(it) } }.takeIf { it > 0 } ?: return
    var yaw by remember { mutableFloatStateOf(0.62f) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 9.sp, color = Muted, fontFamily = FontFamily.Monospace)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    yaw += drag.x * 0.008f
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h * 0.56f
        val cosY = cos(yaw)
        val sinY = sin(yaw)
        // world scale: keep the rotated footprint inside the canvas
        val s = w / (DAYS + BLOCKS) / 1.35f
        val hMax = h * 0.34f

        fun proj(gx: Double, gz: Double, height: Float): Offset {
            val ux = (gx - DAYS / 2.0 + 0.5) * s * 2.2 // stretch day axis
            val uz = (gz - BLOCKS / 2.0 + 0.5) * s
            val rx = ux * cosY - uz * sinY
            val rz = ux * sinY + uz * cosY
            return Offset(cx + rx.toFloat(), cy + rz.toFloat() * 0.5f - height)
        }

        fun depth(gx: Double, gz: Double): Double {
            val ux = (gx - DAYS / 2.0 + 0.5) * s * 2.2
            val uz = (gz - BLOCKS / 2.0 + 0.5) * s
            return ux * sinY + uz * cosY
        }

        // floor platform outline
        val plat = listOf(
            proj(-0.6, -0.6, 0f),
            proj(DAYS - 0.4, -0.6, 0f),
            proj(DAYS - 0.4, BLOCKS - 0.4, 0f),
            proj(-0.6, BLOCKS - 0.4, 0f),
        )
        drawPath(
            Path().apply {
                moveTo(plat[0].x, plat[0].y)
                plat.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            },
            color = OutlineSoft.copy(alpha = 0.35f),
        )

        // bars, painter's order (far → near)
        val order = buildList {
            for (d in 0 until DAYS) for (b in 0 until BLOCKS) add(d to b)
        }.sortedBy { (d, b) -> depth(d.toDouble(), b.toDouble()) }

        order.forEach { (d, b) ->
            val v = grid[d][b]
            if (v == 0.0) return@forEach
            val barH = ((abs(v) / maxAbs) * hMax).toFloat().coerceAtLeast(3f)
            val base = if (v >= 0) Positive else Negative
            val hx = 0.36
            val hz = 0.36
            // 4 corners: (−,−) (+,−) (+,+) (−,+) in grid space
            val cs = listOf(-hx to -hz, hx to -hz, hx to hz, -hx to hz)
            val bot = cs.map { (ox, oz) -> proj(d + ox, b + oz, 0f) }
            val top = cs.map { (ox, oz) -> proj(d + ox, b + oz, barH) }
            // side faces sorted far→near by edge-midpoint depth
            val faces = (0 until 4).sortedBy { i ->
                val j = (i + 1) % 4
                val mx = d + (cs[i].first + cs[j].first) / 2
                val mz = b + (cs[i].second + cs[j].second) / 2
                depth(mx, mz)
            }
            faces.forEachIndexed { rank, i ->
                val j = (i + 1) % 4
                val shade = if (rank < 2) 0.45f else 0.72f // far faces darker
                drawPath(
                    Path().apply {
                        moveTo(bot[i].x, bot[i].y)
                        lineTo(bot[j].x, bot[j].y)
                        lineTo(top[j].x, top[j].y)
                        lineTo(top[i].x, top[i].y)
                        close()
                    },
                    color = Color(
                        base.red * shade,
                        base.green * shade,
                        base.blue * shade,
                        1f,
                    ),
                )
            }
            // top face, lightest
            drawPath(
                Path().apply {
                    moveTo(top[0].x, top[0].y)
                    top.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                },
                color = base,
            )
        }

        // axis labels (rotate with the scene)
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
        for (d in 0 until DAYS) {
            val p = proj(d.toDouble(), -1.3, 0f)
            drawText(textMeasurer, dayNames[d], topLeft = Offset(p.x - 12f, p.y), style = labelStyle)
        }
        for (b in 0 until BLOCKS step 3) {
            val p = proj(DAYS.toDouble() + 0.4, b.toDouble(), 0f)
            drawText(
                textMeasurer,
                "%02d".format(b * 2),
                topLeft = Offset(p.x, p.y),
                style = labelStyle,
            )
        }
    }
}

// ─── shared formatting ─────────────────────────────────────────────────────

private fun moneyAbs(v: Double): String = "$" + "%,.0f".format(abs(v))

private fun pctFmt(v: Double): String {
    val asPct = if (v <= 1.5) v * 100 else v
    return "%.0f%%".format(asPct)
}
