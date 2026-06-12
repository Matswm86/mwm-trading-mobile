package no.mwmai.backtest.feature.results

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.mwmai.backtest.data.model.RunDto
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.OutlineSoft
import no.mwmai.backtest.ui.theme.Positive
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultsScreen(
    jobId: Int,
    onBack: () -> Unit,
    viewModel: ResultsViewModel = viewModel(),
) {
    LaunchedEffect(jobId) { viewModel.start(jobId) }
    val s by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Results", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
            when (val st = s) {
                is ResultsUiState.Polling -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "Job ${st.status}…",
                        modifier = Modifier.padding(top = 12.dp),
                        color = Muted,
                    )
                    Text(
                        "You can leave; a notification fires when it finishes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                is ResultsUiState.Failed -> Text(st.message, color = Negative, modifier = Modifier.padding(24.dp))
                is ResultsUiState.Ready -> ResultBody(st.run, st.stats)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultBody(run: RunDto, stats: no.mwmai.backtest.data.model.RunStats?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            listOfNotNull(run.strategy, run.symbol, run.timeframe).joinToString("  •  "),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "${run.startIso?.take(10)} → ${run.endIso?.take(10)}",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OutlineSoft, RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                .padding(12.dp),
        ) {
            EquityChart(run.equityCurve)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("NET P&L", money(run.netPnl), pnlColor(run.netPnl))
            Tile("TRADES", run.nTrades.toString())
            Tile("WIN", run.winRate?.let { pct(it) } ?: "—")
            Tile("MAX DD", money(-maxDrawdown(run.equityCurve)), Negative)
            Tile("GROSS", money(run.grossPnl))
            Tile("FEES", money(run.commissions?.let { -abs(it) }))
        }

        if (stats == null) {
            Text("Loading stats…", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else {
            StatsSections(stats)
        }
    }
}

@Composable
private fun Tile(label: String, value: String, valueColor: Color? = null) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
            Text(
                value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun pnlColor(v: Double?): Color? = when {
    v == null -> null
    v >= 0 -> Positive
    else -> Negative
}

private fun maxDrawdown(curve: List<Double>): Double {
    if (curve.isEmpty()) return 0.0
    // Baseline 0.0 (flat before trade 1) so an opening drawdown is counted.
    var peak = 0.0
    var mdd = 0.0
    for (v in curve) {
        if (v > peak) peak = v
        if (peak - v > mdd) mdd = peak - v
    }
    return mdd
}

private fun money(v: Double?): String {
    if (v == null) return "—"
    val prefix = if (v < 0) "-$" else "$"
    return prefix + "%,.0f".format(abs(v))
}

private fun pct(v: Double): String {
    val asPct = if (v <= 1.5) v * 100 else v
    return "%.0f%%".format(asPct)
}
