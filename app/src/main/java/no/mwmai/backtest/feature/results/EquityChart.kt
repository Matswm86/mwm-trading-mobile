package no.mwmai.backtest.feature.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.Positive

/** Dependency-free cumulative-equity line (one point per trade). */
@Composable
fun EquityChart(curve: List<Double>, modifier: Modifier = Modifier) {
    if (curve.size < 2) return
    val min = curve.min()
    val max = curve.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val up = curve.last() >= curve.first()
    val color = if (up) Positive else Negative
    val zeroFrac = ((0.0 - min) / span).coerceIn(0.0, 1.0)

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        // zero line if the curve crosses it
        if (min < 0.0 && max > 0.0) {
            val y = h - (zeroFrac * h).toFloat()
            drawLine(Muted, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }
        val path = Path()
        curve.forEachIndexed { i, v ->
            val x = w * i / (curve.size - 1)
            val y = h - (((v - min) / span) * h).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
        )
    }
}
