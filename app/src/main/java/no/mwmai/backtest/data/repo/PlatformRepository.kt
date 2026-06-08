package no.mwmai.backtest.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.mwmai.backtest.data.api.NetworkModule
import no.mwmai.backtest.data.api.PlatformApi
import no.mwmai.backtest.data.model.DataRange
import no.mwmai.backtest.data.model.JobDto
import no.mwmai.backtest.data.model.ObservabilityResponse
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.data.model.PickerStrategy
import no.mwmai.backtest.data.model.RunDto
import no.mwmai.backtest.data.model.SubmitJobRequest
import no.mwmai.backtest.data.model.SubmitJobResponse

class PlatformRepository(
    private val api: PlatformApi = NetworkModule.api,
) {
    suspend fun observability(): Result<ObservabilityResponse> = io { api.getObservability() }

    suspend fun pickerStrategies(): Result<List<PickerStrategy>> = io {
        api.pickerStrategies().strategies
    }

    suspend fun paramSpec(name: String): Result<ParamSpecResponse> = io { api.paramSpec(name) }

    suspend fun dataRange(symbol: String, timeframe: String): Result<DataRange> = io {
        api.dataRange(symbol, timeframe)
    }

    suspend fun submitJob(
        authorization: String,
        body: SubmitJobRequest,
    ): Result<SubmitJobResponse> = io { api.submitJob(authorization, body) }

    suspend fun job(id: Int): Result<JobDto> = io { api.job(id).job }

    suspend fun run(id: String): Result<RunDto> = io { api.run(id).run }

    private suspend fun <T> io(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) { runCatching { block() } }
}
