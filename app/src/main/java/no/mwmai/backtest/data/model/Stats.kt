package no.mwmai.backtest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /api/runs/{id}/stats — standard + advanced (rigor) suites. */
@Serializable
data class RunStats(
    @SerialName("run_id") val runId: String = "",
    @SerialName("coverage_days") val coverageDays: Int = 0,
    val standard: StandardStats? = null,
    val advanced: AdvancedStats? = null,
)

@Serializable
data class StandardStats(
    val available: Boolean = false,
    val reason: String? = null,
    @SerialName("n_trades") val nTrades: Int = 0,
    @SerialName("net_pnl") val netPnl: Double? = null,
    @SerialName("gross_win") val grossWin: Double? = null,
    @SerialName("gross_loss") val grossLoss: Double? = null,
    @SerialName("profit_factor") val profitFactor: Double? = null,
    val expectancy: Double? = null,
    @SerialName("win_rate") val winRate: Double? = null,
    @SerialName("avg_win") val avgWin: Double? = null,
    @SerialName("avg_loss") val avgLoss: Double? = null,
    @SerialName("payoff_ratio") val payoffRatio: Double? = null,
    @SerialName("sharpe_per_trade") val sharpePerTrade: Double? = null,
    @SerialName("sharpe_annual") val sharpeAnnual: Double? = null,
    @SerialName("sortino_annual") val sortinoAnnual: Double? = null,
    @SerialName("max_drawdown") val maxDrawdown: Double? = null,
)

@Serializable
data class AdvancedStats(
    val available: Boolean = false,
    val reason: String? = null,
    @SerialName("n_trials") val nTrials: Int = 1,
    val dsr: Double? = null,
    @SerialName("dsr_pass") val dsrPass: Boolean? = null,
    @SerialName("psr_vs_zero") val psrVsZero: Double? = null,
    @SerialName("min_btl_trades") val minBtlTrades: Double? = null,
    @SerialName("min_btl_sufficient") val minBtlSufficient: Boolean? = null,
    @SerialName("lo_sharpe_per_trade") val loSharpePerTrade: Double? = null,
    @SerialName("lo_sharpe_annual") val loSharpeAnnual: Double? = null,
    @SerialName("naive_sharpe_annual") val naiveSharpeAnnual: Double? = null,
    @SerialName("lo_overstatement_pct") val loOverstatementPct: Double? = null,
    val cpcv: Cpcv? = null,
    val bootstrap: Bootstrap? = null,
)

@Serializable
data class Cpcv(
    val p05: Double? = null,
    val p50: Double? = null,
    val p95: Double? = null,
    @SerialName("pct_profitable") val pctProfitable: Double? = null,
)

@Serializable
data class Bootstrap(
    @SerialName("sharpe_ci") val sharpeCi: List<Double> = emptyList(),
    @SerialName("total_pnl_ci") val totalPnlCi: List<Double> = emptyList(),
    @SerialName("pnl_ci_excludes_zero") val pnlCiExcludesZero: Boolean? = null,
)
