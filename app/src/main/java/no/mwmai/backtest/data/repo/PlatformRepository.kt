package no.mwmai.backtest.data.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import no.mwmai.backtest.data.api.NetworkModule
import no.mwmai.backtest.data.api.PlatformApi
import no.mwmai.backtest.data.model.DataRange
import no.mwmai.backtest.data.model.JobDto
import no.mwmai.backtest.data.model.ObservabilityResponse
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.data.model.PickerStrategy
import no.mwmai.backtest.data.model.RunDto
import no.mwmai.backtest.data.model.RunStats
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

    suspend fun submitJob(body: SubmitJobRequest): Result<SubmitJobResponse> =
        io { api.submitJob(body) }

    suspend fun job(id: Int): Result<JobDto> = io { api.job(id).job }

    suspend fun run(id: String): Result<RunDto> = io { api.run(id).run }

    suspend fun runStats(id: String): Result<RunStats> = io { api.runStats(id) }

    private suspend fun <T> io(block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }.recoverCatching { e -> throw enrich(e) }
        }

    /** Turn an HTTP error into a message that carries the server's error body,
     *  so the UI shows e.g. `HTTP 400: {"error":"…"}` instead of a bare code. */
    private fun enrich(e: Throwable): Throwable = when (e) {
        is HttpException -> {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            RuntimeException("HTTP ${e.code()}: ${body?.take(400) ?: e.message()}", e)
        }
        else -> e
    }
}
