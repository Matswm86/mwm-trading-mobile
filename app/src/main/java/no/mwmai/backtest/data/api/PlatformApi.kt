package no.mwmai.backtest.data.api

import no.mwmai.backtest.data.model.DataRange
import no.mwmai.backtest.data.model.JobEnvelope
import no.mwmai.backtest.data.model.ObservabilityResponse
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.data.model.PickerResponse
import no.mwmai.backtest.data.model.RunEnvelope
import no.mwmai.backtest.data.model.RunStats
import no.mwmai.backtest.data.model.SubmitJobRequest
import no.mwmai.backtest.data.model.SubmitJobResponse
import no.mwmai.backtest.data.model.TradesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * MWM trading platform API. GETs are public (no auth). The single mutating call
 * — POST /api/jobs — is gated by Caddy basic-auth, so it takes an explicit
 * Authorization header built from stored credentials.
 */
interface PlatformApi {

    @GET("api/observability")
    suspend fun getObservability(): ObservabilityResponse

    @GET("api/picker/strategies")
    suspend fun pickerStrategies(): PickerResponse

    @GET("api/get_strategy_param_spec")
    suspend fun paramSpec(@Query("name") name: String): ParamSpecResponse

    @GET("api/data_range")
    suspend fun dataRange(
        @Query("symbol") symbol: String,
        @Query("timeframe") timeframe: String,
    ): DataRange

    // Unauthenticated: POST /api/jobs is compute-only and exempt from auth.
    @POST("api/jobs")
    suspend fun submitJob(@Body body: SubmitJobRequest): SubmitJobResponse

    @GET("api/jobs/{id}")
    suspend fun job(@Path("id") id: Int): JobEnvelope

    @GET("api/runs/{id}")
    suspend fun run(@Path("id") id: String): RunEnvelope

    @GET("api/runs/{id}/stats")
    suspend fun runStats(
        @Path("id") id: String,
        @Query("n_trials") nTrials: Int = 1,
    ): RunStats

    @GET("api/runs/{id}/trades")
    suspend fun runTrades(@Path("id") id: String): TradesResponse
}
