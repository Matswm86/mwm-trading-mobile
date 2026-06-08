package no.mwmai.backtest.data.api

import no.mwmai.backtest.data.model.ObservabilityResponse
import retrofit2.http.GET

/**
 * Read surface of the MWM trading platform API. All endpoints here are public
 * GETs (no auth). Mutating calls (POST /api/jobs) will live in a separate
 * authenticated interface once the run-backtest screen lands.
 */
interface PlatformApi {

    @GET("api/observability")
    suspend fun getObservability(): ObservabilityResponse
}
