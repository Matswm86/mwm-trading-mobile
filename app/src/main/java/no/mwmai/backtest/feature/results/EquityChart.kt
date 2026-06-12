package no.mwmai.backtest.feature.results

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.Positive

/** Dependency-free cumulative-equity line (one point per trade) with a soft
 *  gradient fill under the curve. */
@Composable
fun EquityChart(curve: List<Double>, modifier: Modifier = Modifier) {
    if (curve.size < 2) return
    val min = curve.min()
    val max = curve.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val up = curve.last() >= curve.first()
    val color = if (up) Positive else Negative
    val zeroFrac = ((0.0 - min) / span).coerceIn(0.0, 1.0)

    Canvas(modifier = modifier.fillMaxWidth().height(190.dp)) {
        val w = size.width
        val h = size.height

        fun pt(i: Int): Offset {
            val v = curve[i]
            val x = w * i / (curve.size - 1)
            val y = h - (((v - min) / span) * h).toFloat()
            return Offset(x, y)
        }

        // soft fill under the curve
        val fill = Path().apply {
            moveTo(0f, h)
            for (i in curve.indices) {
                val p = pt(i)
                lineTo(p.x, p.y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
            ),
        )

        // zero line if the curve crosses it
        if (min < 0.0 && max > 0.0) {
            val y = h - (zeroFrac * h).toFloat()
            drawLine(
                Muted.copy(alpha = 0.6f),
                Offset(0f, y),
                Offset(w, y),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
        }

        val line = Path()
        for (i in curve.indices) {
            val p = pt(i)
            if (i == 0) line.moveTo(p.x, p.y) else line.lineTo(p.x, p.y)
        }
        drawPath(
            line,
            color = color,
            style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
