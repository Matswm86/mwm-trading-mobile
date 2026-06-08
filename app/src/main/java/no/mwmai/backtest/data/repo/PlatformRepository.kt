package no.mwmai.backtest.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.mwmai.backtest.data.api.NetworkModule
import no.mwmai.backtest.data.api.PlatformApi
import no.mwmai.backtest.data.model.ObservabilityResponse

class PlatformRepository(
    private val api: PlatformApi = NetworkModule.api,
) {
    suspend fun observability(): Result<ObservabilityResponse> = withContext(Dispatchers.IO) {
        runCatching { api.getObservability() }
    }
}
