package no.mwmai.backtest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lean DTOs for `GET /api/observability` (the cmd.mwmai.no cockpit aggregate).
 * Modelled against the live payload 2026-06-08. The Json parser is configured
 * with ignoreUnknownKeys, so fields not declared here (e.g. equity_curve, whose
 * element types still need confirming for the detail screen) are skipped safely.
 */
@Serializable
data class ObservabilityResponse(
    @SerialName("generated_at") val generatedAt: String? = null,
    val accounts: List<AccountDto> = emptyList(),
    val cells: List<CellDto> = emptyList(),
)

@Serializable
data class AccountDto(
    val id: String = "",
    val label: String = "",
    val kind: String = "",
    val balance: Double? = null,
    val money: String? = null,
    val mll: Int? = null,
    val size: Int? = null,
    val note: String? = null,
)

@Serializable
data class CellDto(
    val name: String = "",
    val account: String? = null,
    val strategy: String? = null,
    val symbol: String? = null,
    @SerialName("fill_basis") val fillBasis: String? = null,
    @SerialName("generated_at") val generatedAt: String? = null,
    @SerialName("composite_score") val compositeScore: Int? = null,
    val backtest: BacktestDto? = null,
    val ironclad: IroncladDto? = null,
    val live: LiveDto? = null,
    val validation: ValidationDto? = null,
)

@Serializable
data class BacktestDto(
    @SerialName("profit_factor") val profitFactor: Double? = null,
    @SerialName("max_drawdown") val maxDrawdown: Double? = null,
    @SerialName("total_pnl") val totalPnl: Double? = null,
    @SerialName("win_rate") val winRate: Double? = null,
    @SerialName("expectancy") val expectancy: Double? = null,
    @SerialName("n_trades") val nTrades: Int? = null,
    @SerialName("coverage_days") val coverageDays: Int? = null,
    @SerialName("short_coverage") val shortCoverage: Boolean? = null,
    @SerialName("data_window") val dataWindow: DataWindowDto? = null,
)

@Serializable
data class DataWindowDto(
    val start: String? = null,
    val end: String? = null,
)

@Serializable
data class IroncladDto(
    val status: String? = null,
    val note: String? = null,
    val source: String? = null,
)

@Serializable
data class LiveDto(
    val health: String? = null,
    val state: String? = null,
    @SerialName("today_pnl") val todayPnl: Double? = null,
    @SerialName("closed_trades_today") val closedTradesToday: Int? = null,
    @SerialName("heartbeat_age_s") val heartbeatAgeS: Double? = null,
    @SerialName("pnl_status") val pnlStatus: String? = null,
)

@Serializable
data class ValidationDto(
    val available: Boolean? = null,
    val dsr: Double? = null,
    @SerialName("dsr_pass") val dsrPass: Boolean? = null,
    @SerialName("lo_sharpe_annual") val loSharpeAnnual: Double? = null,
)
