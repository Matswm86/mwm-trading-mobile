package no.mwmai.backtest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- submit (POST /api/jobs) ----------------------------------------------

@Serializable
data class SubmitJobRequest(
    @SerialName("job_type") val jobType: String = "backtest",
    val config: JobConfig,
)

@Serializable
data class JobConfig(
    @SerialName("cell_spec") val cellSpec: CellSpec,
    val start: String,
    val end: String,
    val notes: String = "",
)

@Serializable
data class CellSpec(
    val strategy: String,
    val symbol: String,
    val timeframe: String,
    // Optional: only field beyond the three the worker honours on ad-hoc cells.
    val contracts: Int? = null,
)

@Serializable
data class SubmitJobResponse(
    @SerialName("job_id") val jobId: Int = 0,
    val status: String = "",
    @SerialName("submitted_by") val submittedBy: String? = null,
)

// --- job status (GET /api/jobs/{id} -> {job:{...}}) ------------------------

@Serializable
data class JobEnvelope(val job: JobDto)

@Serializable
data class JobDto(
    val id: Int = 0,
    @SerialName("job_type") val jobType: String = "",
    val status: String = "",
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("run_id") val runId: String? = null,
    val error: String? = null,
) {
    val isTerminal: Boolean get() = status in TERMINAL
    val isSuccess: Boolean get() = status == "completed" && runId != null

    companion object {
        val TERMINAL = setOf("completed", "failed", "cancelled")
    }
}

// --- run result (GET /api/runs/{id} -> {run:{...}}) ------------------------

@Serializable
data class RunEnvelope(val run: RunDto)

@Serializable
data class RunDto(
    val id: String = "",
    val strategy: String? = null,
    @SerialName("cell_name") val cellName: String? = null,
    val symbol: String? = null,
    val timeframe: String? = null,
    @SerialName("start_iso") val startIso: String? = null,
    @SerialName("end_iso") val endIso: String? = null,
    @SerialName("n_trades") val nTrades: Int = 0,
    @SerialName("win_rate") val winRate: Double? = null,
    @SerialName("gross_pnl_usd") val grossPnl: Double? = null,
    @SerialName("commissions_usd") val commissions: Double? = null,
    @SerialName("net_pnl_usd") val netPnl: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Double? = null,
    @SerialName("equity_curve") val equityCurve: List<Double> = emptyList(),
)
