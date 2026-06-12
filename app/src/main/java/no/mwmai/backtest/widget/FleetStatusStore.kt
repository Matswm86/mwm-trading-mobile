package no.mwmai.backtest.widget

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.mwmai.backtest.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Fleet heartbeat feed for the home-screen widget.
 *
 * GET /api/fleet-status is a static JSON written every 60s by the VPS
 * fleet-status timer and served straight from Caddy — it stays up even when
 * the platform backend is down, which is exactly when the widget matters.
 * Payload is public + secrets-free (no account IDs / balances / PnL).
 */
@Serializable
data class FleetMarketDto(
    val open: Boolean = false,
    val state: String = "?",
    val next_change_utc: String? = null,
)

@Serializable
data class FleetGroupDto(
    val key: String,
    val label: String,
    val status: String,
    val ok: Int,
    val total: Int,
)

@Serializable
data class FleetInfraDto(
    val name: String,
    val status: String,
)

@Serializable
data class FleetStatusDto(
    val generated_at: String,
    val market: FleetMarketDto = FleetMarketDto(),
    val overall: String = "?",
    val groups: List<FleetGroupDto> = emptyList(),
    val infra: List<FleetInfraDto> = emptyList(),
) {
    val generatedAt: Instant?
        get() = runCatching { Instant.parse(generated_at) }.getOrNull()
}

object FleetStatusStore {
    private const val PREFS = "fleet_status_widget"
    private const val KEY_JSON = "raw_json"
    private const val KEY_FETCHED_AT = "fetched_at_ms"
    private const val KEY_FETCH_OK = "fetch_ok"

    /** Server ticks every 60s; older than this and the feed itself is sick. */
    const val SERVER_STALE_MS = 10 * 60 * 1000L

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /** Fetch + persist. Returns true on success; last good payload is kept on failure. */
    fun refresh(context: Context): Boolean {
        val ok = runCatching {
            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "api/fleet-status")
                .build()
            client.newCall(req).execute().use { resp ->
                check(resp.isSuccessful) { "HTTP ${resp.code}" }
                val body = resp.body?.string().orEmpty()
                json.decodeFromString<FleetStatusDto>(body) // validate before storing
                prefs(context).edit().putString(KEY_JSON, body).apply()
            }
        }.isSuccess
        prefs(context).edit()
            .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
            .putBoolean(KEY_FETCH_OK, ok)
            .apply()
        return ok
    }

    data class Snapshot(
        val status: FleetStatusDto?,
        val fetchedAtMs: Long,
        val fetchOk: Boolean,
    )

    fun snapshot(context: Context): Snapshot {
        val p = prefs(context)
        val dto = p.getString(KEY_JSON, null)?.let {
            runCatching { json.decodeFromString<FleetStatusDto>(it) }.getOrNull()
        }
        return Snapshot(dto, p.getLong(KEY_FETCHED_AT, 0L), p.getBoolean(KEY_FETCH_OK, false))
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
