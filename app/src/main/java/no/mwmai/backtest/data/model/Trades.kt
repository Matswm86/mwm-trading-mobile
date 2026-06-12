package no.mwmai.backtest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GET /api/runs/{id}/trades — full trade log for a run. */
@Serializable
data class TradesResponse(
    @SerialName("run_id") val runId: String = "",
    val trades: List<TradeDto> = emptyList(),
)

@Serializable
data class TradeDto(
    val side: String? = null,
    val qty: Int? = null,
    @SerialName("entry_ts") val entryTs: String? = null,
    @SerialName("entry_px") val entryPx: Double? = null,
    @SerialName("exit_ts") val exitTs: String? = null,
    @SerialName("exit_px") val exitPx: Double? = null,
    @SerialName("exit_reason") val exitReason: String? = null,
    @SerialName("pnl_usd") val pnlUsd: Double? = null,
    @SerialName("commission_usd") val commissionUsd: Double? = null,
)
