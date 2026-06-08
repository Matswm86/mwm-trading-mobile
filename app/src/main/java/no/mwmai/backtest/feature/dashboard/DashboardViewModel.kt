package no.mwmai.backtest.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.mwmai.backtest.data.model.AccountDto
import no.mwmai.backtest.data.model.CellDto
import no.mwmai.backtest.data.repo.PlatformRepository

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(
        val accounts: List<AccountDto>,
        val cells: List<CellDto>,
    ) : DashboardUiState
}

class DashboardViewModel(
    private val repo: PlatformRepository = PlatformRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _refreshing.value = true
            repo.observability()
                .onSuccess { resp ->
                    _state.value = DashboardUiState.Ready(resp.accounts, resp.cells)
                }
                .onFailure { e ->
                    _state.value = DashboardUiState.Error(e.message ?: "Network error")
                }
            _refreshing.value = false
        }
    }
}
