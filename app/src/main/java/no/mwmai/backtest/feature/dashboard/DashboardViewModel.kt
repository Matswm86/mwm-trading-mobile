package no.mwmai.backtest.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.mwmai.backtest.data.model.AccountDto
import no.mwmai.backtest.data.model.CellDto
import no.mwmai.backtest.data.model.ObservabilityResponse
import no.mwmai.backtest.data.repo.PlatformRepository

/** Cells grouped under the account they run on, in API account order. */
data class AccountGroup(
    val accountId: String,
    val label: String,
    val isLiveMoney: Boolean,
    val note: String?,
    val cells: List<CellDto>,
) {
    val todayPnl: Double? =
        cells.mapNotNull { it.live?.todayPnl }.takeIf { it.isNotEmpty() }?.sum()
}

data class FleetSummary(
    val nCells: Int,
    val nLiveMoneyCells: Int,
    val todayPnl: Double?,
    val generatedAt: String?,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(val groups: List<AccountGroup>, val fleet: FleetSummary) : DashboardUiState
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
                .onSuccess { resp -> _state.value = DashboardUiState.Ready(group(resp), fleet(resp)) }
                .onFailure { e -> _state.value = DashboardUiState.Error(e.message ?: "Network error") }
            _refreshing.value = false
        }
    }

    private fun group(resp: ObservabilityResponse): List<AccountGroup> {
        val byId: Map<String, AccountDto> = resp.accounts.associateBy { it.id }
        val cellsByAccount = resp.cells.groupBy { it.account ?: "?" }
        // Server account order first (live money leads there), then any account
        // ids that appear on cells but not in the reference list.
        val ordered = resp.accounts.map { it.id }.filter { it in cellsByAccount } +
            cellsByAccount.keys.filter { id -> resp.accounts.none { it.id == id } }
        return ordered.map { id ->
            val acc = byId[id]
            AccountGroup(
                accountId = id,
                label = acc?.label ?: "Account $id",
                isLiveMoney = acc?.money == "live",
                note = acc?.note,
                cells = cellsByAccount[id].orEmpty(),
            )
        }
    }

    private fun fleet(resp: ObservabilityResponse): FleetSummary {
        val liveIds = resp.accounts.filter { it.money == "live" }.map { it.id }.toSet()
        val pnls = resp.cells.mapNotNull { it.live?.todayPnl }
        return FleetSummary(
            nCells = resp.cells.size,
            nLiveMoneyCells = resp.cells.count { it.account in liveIds },
            todayPnl = pnls.takeIf { it.isNotEmpty() }?.sum(),
            generatedAt = resp.generatedAt,
        )
    }
}
