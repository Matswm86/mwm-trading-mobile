package no.mwmai.backtest.feature.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.mwmai.backtest.data.model.AdvancedStats
import no.mwmai.backtest.data.model.RunStats
import no.mwmai.backtest.data.model.StandardStats
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative
import no.mwmai.backtest.ui.theme.Positive
import no.mwmai.backtest.ui.theme.Warn
import kotlin.math.abs

@Composable
fun StatsSections(stats: RunStats) {
    stats.standard?.let { StandardCard(it) }
    stats.advanced?.let { AdvancedCard(it, stats.coverageDays) }
}

@Composable
private fun StandardCard(s: StandardStats) {
    StatCard("Standard metrics") {
        if (!s.available) {
            Text(s.reason ?: "unavailable", color = Muted, style = MaterialTheme.typography.bodySmall)
            return@StatCard
        }
        StatRow("Profit factor", num(s.profitFactor, 2))
        StatRow("Expectancy / trade", money(s.expectancy))
        StatRow("Sharpe (annual)", num(s.sharpeAnnual, 2))
        StatRow("Sharpe (per trade)", num(s.sharpePerTrade, 3))
        StatRow("Sortino (annual)", num(s.sortinoAnnual, 2))
        StatRow("Win rate", pct(s.winRate))
        StatRow("Avg win / loss", "${money(s.avgWin)} / ${money(s.avgLoss)}")
        StatRow("Payoff ratio", num(s.payoffRatio, 2))
        StatRow("Gross win / loss", "${money(s.grossWin)} / ${money(s.grossLoss)}")
        StatRow("Max drawdown", money(s.maxDrawdown?.let { -abs(it) }), Negative)
    }
}

@Composable
private fun AdvancedCard(a: AdvancedStats, coverageDays: Int) {
    StatCard("Advanced (rigor)") {
        if (!a.available) {
            Text(
                a.reason ?: "unavailable",
                color = Warn,
                style = MaterialTheme.typography.bodySmall,
            )
            return@StatCard
        }
        Text(
            "n_trials = ${a.nTrials} (single run; no parameter-search deflation). " +
                "Coverage ${coverageDays}d.",
            style = MaterialTheme.typography.labelSmall,
            color = Muted,
        )
        StatRow("Deflated Sharpe", num(a.dsr, 3), passColor(a.dsrPass))
        StatRow("  DSR verdict", if (a.dsrPass == true) "PASS" else "FAIL", passColor(a.dsrPass))
        StatRow("PSR vs zero", num(a.psrVsZero, 3))
        StatRow("Lo-adj Sharpe (annual)", num(a.loSharpeAnnual, 2))
        StatRow("Naive Sharpe (annual)", num(a.naiveSharpeAnnual, 2))
        StatRow("Overstatement", a.loOverstatementPct?.let { "%.1f%%".format(it) } ?: "—")
        StatRow(
            "Min track record",
            a.minBtlTrades?.let { "${num(it, 1)} trades" } ?: "—",
            passColor(a.minBtlSufficient),
        )
        a.cpcv?.let { c ->
            StatRow("CPCV Sharpe p05/p50/p95", "${num(c.p05, 2)} / ${num(c.p50, 2)} / ${num(c.p95, 2)}")
            StatRow("CPCV % profitable paths", c.pctProfitable?.let { "%.0f%%".format(it) } ?: "—")
        }
        a.bootstrap?.let { b ->
            StatRow("Bootstrap Sharpe CI", ci(b.sharpeCi, 2))
            StatRow("Bootstrap PnL CI", ci(b.totalPnlCi, 0, asMoney = true), passColor(b.pnlCiExcludesZero))
        }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun passColor(ok: Boolean?): Color? = when (ok) {
    true -> Positive
    false -> Warn
    null -> null
}

private fun num(v: Double?, decimals: Int): String =
    if (v == null) "—" else "%.${decimals}f".format(v)

private fun pct(v: Double?): String {
    if (v == null) return "—"
    val asPct = if (v <= 1.5) v * 100 else v
    return "%.0f%%".format(asPct)
}

private fun money(v: Double?): String {
    if (v == null) return "—"
    val prefix = if (v < 0) "-$" else "$"
    return prefix + "%,.0f".format(abs(v))
}

private fun ci(bounds: List<Double>, decimals: Int, asMoney: Boolean = false): String {
    if (bounds.size < 2) return "—"
    val fmt: (Double) -> String =
        if (asMoney) { x -> money(x) } else { x -> "%.${decimals}f".format(x) }
    return "[${fmt(bounds[0])}, ${fmt(bounds[1])}]"
}
