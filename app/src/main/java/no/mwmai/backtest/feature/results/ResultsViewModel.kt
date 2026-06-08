package no.mwmai.backtest.feature.results

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.mwmai.backtest.data.model.RunDto
import no.mwmai.backtest.data.model.RunStats
import no.mwmai.backtest.data.repo.PlatformRepository

sealed interface ResultsUiState {
    data class Polling(val status: String) : ResultsUiState
    data class Failed(val message: String) : ResultsUiState
    data class Ready(val run: RunDto, val stats: RunStats? = null) : ResultsUiState
}

class ResultsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PlatformRepository()
    private val _state = MutableStateFlow<ResultsUiState>(ResultsUiState.Polling("pending"))
    val state: StateFlow<ResultsUiState> = _state.asStateFlow()

    private var started = false

    fun start(jobId: Int) {
        if (started) return
        started = true
        viewModelScope.launch {
            repeat(MAX_POLLS) {
                val job = repo.job(jobId).getOrNull()
                when {
                    job == null -> Unit // transient; retry
                    job.isSuccess -> {
                        val runId = job.runId!!
                        val run = repo.run(runId).getOrNull()
                        if (run == null) {
                            _state.value =
                                ResultsUiState.Failed("Job done but result could not be loaded.")
                            return@launch
                        }
                        _state.value = ResultsUiState.Ready(run) // show P&L immediately
                        repo.runStats(runId).onSuccess { st ->
                            (_state.value as? ResultsUiState.Ready)?.let {
                                _state.value = it.copy(stats = st)
                            }
                        }
                        return@launch
                    }
                    job.status == "failed" ->
                        { _state.value = ResultsUiState.Failed(job.error ?: "Backtest failed."); return@launch }
                    job.status == "cancelled" ->
                        { _state.value = ResultsUiState.Failed("Job was cancelled."); return@launch }
                    else -> _state.value = ResultsUiState.Polling(job.status)
                }
                delay(POLL_MS)
            }
            // Still running after the in-app budget; the WorkManager poll will notify.
            _state.value = ResultsUiState.Polling("running (notification will fire when done)")
        }
    }

    companion object {
        private const val MAX_POLLS = 40
        private const val POLL_MS = 3_000L
    }
}
