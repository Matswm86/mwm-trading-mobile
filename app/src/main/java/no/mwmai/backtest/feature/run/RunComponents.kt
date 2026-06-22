package no.mwmai.backtest.feature.run

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import no.mwmai.backtest.data.model.ParamSpec
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.OutlineSoft
import no.mwmai.backtest.ui.theme.Warn

// Picker-controlled / cost-basis keys the user can't override here (the backend
// drops them too): timeframe has its own chip; the cost basis is fixed per
// instrument so a backtest can't run with unrealistic economics.
private val PROTECTED_PARAMS = setOf(
    "timeframe",
    "tick_size",
    "point_value",
    "commission_per_contract",
    "slippage_ticks",
)

/**
 * Editable view of the strategy's tunable parameters. Each control emits a
 * correctly-typed override (number / bool / string) via [onOverride], or null
 * to clear it back to the strategy default. Overrides ride along in
 * cell_spec.params and are validated server-side against the PARAM_SPEC.
 */
@Composable
fun ParamOverridesSection(
    ps: ParamSpecResponse,
    overrides: Map<String, JsonElement>,
    onOverride: (String, JsonElement?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val editable = ps.paramSpec.filter { it.name !in PROTECTED_PARAMS }
    if (editable.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OutlineSoft),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val changed = overrides.size
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) "Hide parameters"
                    else "Tune ${editable.size} parameters" +
                        if (changed > 0) "  ($changed changed)" else "",
                )
            }
            ps.edgeThesis?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (expanded) {
                Text(
                    "1m is the validated edge. Other timeframes and tuned parameters are " +
                        "research-grade (not Ironclad-validated). Fills always step on 1-minute bars.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warn,
                )
                editable.groupBy { it.group ?: "other" }.forEach { (group, params) ->
                    Text(
                        group.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    params.forEach { ParamEditRow(it, overrides[it.name], onOverride) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ParamEditRow(
    p: ParamSpec,
    current: JsonElement?,
    onOverride: (String, JsonElement?) -> Unit,
) {
    val label = p.label.ifBlank { p.name }
    val defStr = p.default?.jsonPrimitive?.content ?: ""

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            label + if (current != null) "  • changed" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (current != null) MaterialTheme.colorScheme.primary else Muted,
        )
        when {
            p.kind == "boolean" -> {
                val defBool = p.default?.jsonPrimitive?.booleanOrNull ?: false
                val cur = current?.jsonPrimitive?.booleanOrNull ?: defBool
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = cur,
                        onCheckedChange = { v ->
                            onOverride(p.name, if (v == defBool) null else JsonPrimitive(v))
                        },
                    )
                    Text(
                        if (cur) "On" else "Off",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            p.kind == "enum" && !p.choices.isNullOrEmpty() -> {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val curStr = current?.jsonPrimitive?.content ?: defStr
                    p.choices!!.forEach { c ->
                        FilterChip(
                            selected = c == curStr,
                            onClick = {
                                onOverride(p.name, if (c == defStr) null else JsonPrimitive(c))
                            },
                            label = { Text(c) },
                        )
                    }
                }
            }

            p.kind == "integer" || p.kind == "decimal" -> {
                val text = current?.jsonPrimitive?.content ?: ""
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        val t = raw.trim()
                        when {
                            t.isEmpty() -> onOverride(p.name, null)
                            p.kind == "integer" ->
                                t.toIntOrNull()?.let { onOverride(p.name, JsonPrimitive(it)) }
                            else ->
                                t.toDoubleOrNull()?.let { onOverride(p.name, JsonPrimitive(it)) }
                        }
                    },
                    singleLine = true,
                    placeholder = { Text("default $defStr") },
                    supportingText = rangeHint(p),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (p.kind == "integer") KeyboardType.Number else KeyboardType.Decimal,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            else -> {
                // time / text / fallback
                val text = current?.jsonPrimitive?.content ?: ""
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        if (raw.isEmpty()) onOverride(p.name, null)
                        else onOverride(p.name, JsonPrimitive(raw))
                    },
                    singleLine = true,
                    placeholder = { Text("default $defStr") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun rangeHint(p: ParamSpec): (@Composable () -> Unit)? {
    val lo = p.minimum
    val hi = p.maximum
    if (lo == null && hi == null) return null
    val parts = buildList {
        if (lo != null) add("min ${fmt(lo)}")
        if (hi != null) add("max ${fmt(hi)}")
    }
    return { Text(parts.joinToString(", "), fontFamily = FontFamily.Monospace) }
}

private fun fmt(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
