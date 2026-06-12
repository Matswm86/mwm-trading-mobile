package no.mwmai.backtest.feature.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.mwmai.backtest.data.model.CellDto
import no.mwmai.backtest.ui.theme.Accent
import no.mwmai.backtest.ui.theme.HeroEdge
import no.mwmai.backtest.ui.theme.HeroGradient
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.OutlineSoft
import no.mwmai.backtest.ui.theme.Positive
import no.mwmai.backtest.ui.theme.Warn
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNewBacktest: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("MWM", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text(" Backtest", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewBacktest,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New backtest") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::load,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                is DashboardUiState.Loading -> CenterBox { CircularProgressIndicator() }
                is DashboardUiState.Error -> CenterBox {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.message, color = Negative)
                        TextButton(onClick = viewModel::load) { Text("Retry") }
                    }
                }
                is DashboardUiState.Ready -> DashboardList(s.groups, s.fleet)
            }
        }
    }
}

@Composable
private fun DashboardList(groups: List<AccountGroup>, fleet: FleetSummary) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { FleetCard(fleet) }
        groups.forEach { g ->
            item(key = "hdr-${g.accountId}") { AccountHeader(g) }
            items(g.cells, key = { "${g.accountId}-${it.name}" }) { CellCard(it) }
        }
    }
}

// ── Fleet summary hero ─────────────────────────────────────────────────────

@Composable
private fun FleetCard(fleet: FleetSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, HeroEdge, RoundedCornerShape(20.dp))
            .background(HeroGradient, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("FLEET", style = MaterialTheme.typography.labelMedium, color = Muted)
            fleet.generatedAt?.let {
                Text(
                    "updated ${localTime(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Muted,
                )
            }
        }
        Text(
            "${fleet.nCells} cells",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "${fleet.nLiveMoneyCells} on live accounts · " +
                "${fleet.nCells - fleet.nLiveMoneyCells} on practice",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ── Account section header ─────────────────────────────────────────────────

@Composable
private fun AccountHeader(g: AccountGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (g.isLiveMoney) Positive else Muted, CircleShape),
        )
        Text(
            g.label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 8.dp),
        )
        Badge(
            text = if (g.isLiveMoney) "LIVE" else "PRAC",
            color = if (g.isLiveMoney) Positive else Muted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${g.cells.size}",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = Muted,
        )
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .padding(start = 8.dp)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

// ── Cell card ──────────────────────────────────────────────────────────────

@Composable
private fun CellCard(c: CellDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, OutlineSoft, RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SymbolChip(c.symbol)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    c.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    c.strategy ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }

        val spark = c.backtest?.equityCurve?.mapNotNull { it.equity }.orEmpty()
        if (spark.size >= 2) {
            Sparkline(
                spark,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Metric("PF", c.backtest?.profitFactor?.let { fmt(it, 2) } ?: "—", Modifier.weight(1f))
            Metric(
                "NET",
                c.backtest?.totalPnl?.let { money(it) } ?: "—",
                Modifier.weight(1f),
                valueColor = pnlColor(c.backtest?.totalPnl),
            )
            Metric(
                "MAX DD",
                c.backtest?.maxDrawdown?.let { money(-abs(it)) } ?: "—",
                Modifier.weight(1f),
                valueColor = Negative,
            )
            Metric("WIN", c.backtest?.winRate?.let { pct(it) } ?: "—", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            c.compositeScore?.let { ScoreBar(it, Modifier.weight(1f)) }
                ?: Spacer(Modifier.weight(1f))
            IroncladChip(c.ironclad?.status)
        }
    }
}

@Composable
private fun SymbolChip(symbol: String?) {
    val color = symbolColor(symbol)
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol ?: "?",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun ScoreBar(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 70 -> Positive
        score >= 50 -> Warn
        else -> Negative
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("SCORE", style = MaterialTheme.typography.labelSmall, color = Muted)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score / 100f).coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
        Text(
            score.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = color,
        )
    }
}

@Composable
private fun IroncladChip(status: String?) {
    val (text, color) = when (status) {
        "locked" -> "LOCKED" to Positive
        "not_locked" -> "VALIDATION" to Muted
        null -> return
        else -> status.uppercase() to Warn
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .padding(start = 10.dp)
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** One-year equity sparkline with a soft fill, colored by direction. */
@Composable
private fun Sparkline(values: List<Double>, modifier: Modifier = Modifier) {
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    val color = if (values.last() >= values.first()) Positive else Negative
    Canvas(modifier = modifier.height(34.dp)) {
        val w = size.width
        val h = size.height
        val line = Path()
        values.forEachIndexed { i, v ->
            val x = w * i / (values.size - 1)
            val y = h - (((v - min) / span) * h).toFloat()
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.0f)),
            ),
        )
        drawPath(line, color = color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

// --- formatting -----------------------------------------------------------

private fun symbolColor(symbol: String?): Color = when (symbol) {
    "MNQ" -> Accent
    "MGC" -> Warn
    "MYM" -> Color(0xFFA78BFA)
    "M2K" -> Color(0xFFF472B6)
    "MES" -> Positive
    else -> Muted
}

private fun localTime(iso: String): String = runCatching {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(iso.take(16).replace('T', ' '))

private fun pnlColor(v: Double?): Color? = when {
    v == null -> null
    v >= 0 -> Positive
    else -> Negative
}

private fun fmt(v: Double, decimals: Int): String = "%.${decimals}f".format(v)

private fun pct(v: Double): String {
    // win_rate arrives as a fraction (0..1) from the backtest metrics.
    val asPct = if (v <= 1.5) v * 100 else v
    return "%.0f%%".format(asPct)
}

private fun money(v: Double): String {
    val prefix = if (v < 0) "-$" else "$"
    return prefix + "%,.0f".format(abs(v))
}
