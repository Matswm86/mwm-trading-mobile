package no.mwmai.backtest.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** GET /api/picker/strategies */
@Serializable
data class PickerResponse(val strategies: List<PickerStrategy> = emptyList())

@Serializable
data class PickerStrategy(
    val key: String = "",
    val label: String = "",
    val timeframes: List<String> = emptyList(),
)

/** GET /api/get_strategy_param_spec?name= */
@Serializable
data class ParamSpecResponse(
    val strategy: String = "",
    val description: String? = null,
    @SerialName("edge_thesis") val edgeThesis: String? = null,
    @SerialName("typical_session") val typicalSession: String? = null,
    @SerialName("risk_notes") val riskNotes: String? = null,
    @SerialName("param_spec") val paramSpec: List<ParamSpec> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class ParamSpec(
    val name: String = "",
    val kind: String = "",
    val default: JsonElement? = null,
    val label: String = "",
    val description: String? = null,
    val minimum: Double? = null,
    val maximum: Double? = null,
    val step: Double? = null,
    val choices: List<String>? = null,
    val group: String? = null,
)

/** GET /api/data_range?symbol=&timeframe= — bounds the date window. */
@Serializable
data class DataRange(
    val symbol: String = "",
    @SerialName("store_symbol") val storeSymbol: String? = null,
    @SerialName("store_timeframe") val storeTimeframe: String? = null,
    @SerialName("first_ts") val firstTs: String? = null,
    @SerialName("last_ts") val lastTs: String? = null,
    @SerialName("n_bars") val nBars: Long = 0,
) {
    val hasData: Boolean get() = nBars > 0 && firstTs != null && lastTs != null
}
