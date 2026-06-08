package no.mwmai.backtest.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.mwmai.backtest.data.model.AccountDto
import no.mwmai.backtest.data.model.CellDto
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.Positive
import no.mwmai.backtest.ui.theme.Warn
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
        topBar = {
            TopAppBar(
                title = { Text("MWM Backtest", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
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
                is DashboardUiState.Ready -> DashboardList(s.accounts, s.cells)
            }
        }
    }
}

@Composable
private fun DashboardList(accounts: List<AccountDto>, cells: List<CellDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (accounts.isNotEmpty()) {
            item { SectionLabel("Accounts") }
            items(accounts, key = { it.id }) { AccountCard(it) }
        }
        item { SectionLabel("Cells (${cells.size})") }
        items(cells, key = { it.name }) { CellCard(it) }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun AccountCard(a: AccountDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(a.label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(a.kind, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Text(
                text = a.money ?: a.balance?.let { money(it) } ?: "—",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CellCard(c: CellDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(c.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        listOfNotNull(c.symbol, c.strategy, c.account).joinToString("  •  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
                HealthDot(c.live?.health)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ironclad: ${c.ironclad?.status ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                Text(
                    text = c.fillBasis?.let { "fill: $it" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)) {
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

@Composable
private fun HealthDot(health: String?) {
    val color = when (health?.lowercase()) {
        "ok", "healthy", "green" -> Positive
        "warn", "warning", "stale", "yellow" -> Warn
        null -> Muted
        else -> Negative
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

// --- formatting -----------------------------------------------------------

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
