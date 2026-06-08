package no.mwmai.backtest.feature.run

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.mwmai.backtest.data.model.CellSpec
import no.mwmai.backtest.data.model.DataRange
import no.mwmai.backtest.data.model.JobConfig
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.data.model.PickerStrategy
import no.mwmai.backtest.data.model.SubmitJobRequest
import no.mwmai.backtest.data.repo.PlatformRepository
import no.mwmai.backtest.work.BacktestPollWorker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Instruments confirmed to have data in the store (probed 2026-06-08). */
val INSTRUMENTS = listOf("MNQ", "MGC", "MES", "BTC")

enum class WindowPreset(val label: String, val days: Long?) {
    M3("3M", 90), M6("6M", 180), Y1("1Y", 365), Y2("2Y", 730), MAX("Max", null)
}

data class RunUiState(
    val loadingCatalog: Boolean = true,
    val strategies: List<PickerStrategy> = emptyList(),
    val selectedStrategy: PickerStrategy? = null,
    val timeframe: String? = null,
    val instrument: String? = null,
    val contracts: String = "",
    val window: WindowPreset = WindowPreset.M3,
    val dataRange: DataRange? = null,
    val loadingRange: Boolean = false,
    val paramSpec: ParamSpecResponse? = null,
    val error: String? = null,
    val submitting: Boolean = false,
    // Set when the job is queued: navigate to results/{jobId}.
    val submittedJobId: Int? = null,
) {
    val availableTimeframes: List<String> get() = selectedStrategy?.timeframes ?: emptyList()
    val canSubmit: Boolean
        get() = selectedStrategy != null && timeframe != null && instrument != null &&
            dataRange?.hasData == true && !submitting
}

class RunBacktestViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PlatformRepository()
    private val isoFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state.asStateFlow()

    init { loadCatalog() }

    fun loadCatalog() {
        viewModelScope.launch {
            _state.update { it.copy(loadingCatalog = true, error = null) }
            repo.pickerStrategies()
                .onSuccess { list -> _state.update { it.copy(loadingCatalog = false, strategies = list) } }
                .onFailure { e -> _state.update { it.copy(loadingCatalog = false, error = e.message) } }
        }
    }

    fun selectStrategy(s: PickerStrategy) {
        val tf = s.timeframes.firstOrNull()
        _state.update { it.copy(selectedStrategy = s, timeframe = tf, paramSpec = null) }
        viewModelScope.launch {
            repo.paramSpec(s.key).onSuccess { ps -> _state.update { it.copy(paramSpec = ps) } }
        }
        refreshRange()
    }

    fun selectTimeframe(tf: String) {
        _state.update { it.copy(timeframe = tf) }
        refreshRange()
    }

    fun selectInstrument(sym: String) {
        _state.update { it.copy(instrument = sym) }
        refreshRange()
    }

    fun setContracts(v: String) {
        _state.update { it.copy(contracts = v.filter(Char::isDigit).take(2)) }
    }

    fun setWindow(w: WindowPreset) = _state.update { it.copy(window = w) }

    private fun refreshRange() {
        val s = _state.value
        val sym = s.instrument ?: return
        val tf = s.timeframe ?: return
        viewModelScope.launch {
            _state.update { it.copy(loadingRange = true) }
            repo.dataRange(sym, tf)
                .onSuccess { r -> _state.update { it.copy(loadingRange = false, dataRange = r) } }
                .onFailure { e -> _state.update { it.copy(loadingRange = false, error = e.message) } }
        }
    }

    fun submit() {
        val s = _state.value
        val strat = s.selectedStrategy ?: return
        val tf = s.timeframe ?: return
        val sym = s.instrument ?: return
        val range = s.dataRange?.takeIf { it.hasData } ?: return
        val (start, end) = resolveWindow(range, s.window)
        val body = SubmitJobRequest(
            config = JobConfig(
                cellSpec = CellSpec(
                    strategy = strat.key,
                    symbol = sym,
                    timeframe = tf,
                    contracts = s.contracts.toIntOrNull()?.takeIf { it > 0 },
                ),
                start = start,
                end = end,
            ),
        )
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            repo.submitJob(body)
                .onSuccess { resp ->
                    enqueuePoll(resp.jobId, "${strat.label} · $sym $tf")
                    _state.update { it.copy(submitting = false, submittedJobId = resp.jobId) }
                }
                .onFailure { e -> _state.update { it.copy(submitting = false, error = e.message) } }
        }
    }

    fun consumeNavigation() = _state.update { it.copy(submittedJobId = null) }

    private fun enqueuePoll(jobId: Int, label: String) {
        val req = OneTimeWorkRequestBuilder<BacktestPollWorker>()
            .setInputData(
                workDataOf(
                    BacktestPollWorker.KEY_JOB_ID to jobId,
                    BacktestPollWorker.KEY_LABEL to label,
                ),
            )
            .build()
        WorkManager.getInstance(getApplication()).enqueue(req)
    }

    private fun resolveWindow(range: DataRange, preset: WindowPreset): Pair<String, String> {
        val first = LocalDateTime.parse(range.firstTs)
        val last = LocalDateTime.parse(range.lastTs)
        val endDate = last.toLocalDate()
        val startDate = when (val d = preset.days) {
            null -> first.toLocalDate()
            else -> maxOf(first.toLocalDate(), endDate.minusDays(d))
        }
        return startDate.atStartOfDay().format(isoFmt) to endDate.atStartOfDay().format(isoFmt)
    }
}
