package no.mwmai.backtest.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonElement
import no.mwmai.backtest.data.model.ParamSpec
import no.mwmai.backtest.data.model.ParamSpecResponse
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Warn

/**
 * Read-only view of the strategy's parameter defaults. The ad-hoc job path runs
 * with these defaults (only strategy / symbol / timeframe / contracts / window
 * are honoured today), so they are shown but not editable — editing them would
 * silently no-op until per-param overrides are plumbed through the API.
 */
@Composable
fun ParamDefaultsSection(ps: ParamSpecResponse) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide strategy defaults" else "Show strategy defaults (${ps.count})")
            }
            ps.edgeThesis?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            if (expanded) {
                Text(
                    "These defaults are applied as-is. Per-parameter tuning is not yet wired " +
                        "through the ad-hoc API (only contracts is adjustable).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warn,
                )
                ps.paramSpec.groupBy { it.group ?: "other" }.forEach { (group, params) ->
                    Text(
                        group.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    params.forEach { ParamRow(it) }
                }
            }
        }
    }
}

@Composable
private fun ParamRow(p: ParamSpec) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(p.label.ifBlank { p.name }, style = MaterialTheme.typography.bodySmall)
        Text(
            renderDefault(p.default),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Muted,
        )
    }
}

private fun renderDefault(e: JsonElement?): String =
    e?.toString()?.removeSurrounding("\"") ?: "—"

@Composable
fun CredentialsDialog(onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Platform credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Queuing a backtest needs the platform basic-auth login. Stored encrypted on-device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(user.trim(), pass) },
                enabled = user.isNotBlank() && pass.isNotBlank(),
            ) { Text("Save & run") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
