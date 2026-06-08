package no.mwmai.backtest.feature.run

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.mwmai.backtest.data.model.PickerStrategy
import no.mwmai.backtest.ui.theme.Muted
import no.mwmai.backtest.ui.theme.Negative

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RunBacktestScreen(
    onBack: () -> Unit,
    onSubmitted: (Int) -> Unit,
    viewModel: RunBacktestViewModel = viewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(s.submittedJobId) {
        s.submittedJobId?.let {
            onSubmitted(it)
            viewModel.consumeNavigation()
        }
    }

    if (s.needsCredentials) {
        CredentialsDialog(
            onSave = viewModel::saveCredentials,
            onDismiss = viewModel::dismissCredentials,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New backtest", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (s.loadingCatalog) {
                CircularProgressIndicator()
                return@Column
            }

            Field("Strategy") {
                Picker(
                    selected = s.selectedStrategy?.label ?: "Select strategy",
                    options = s.strategies,
                    optionLabel = PickerStrategy::label,
                    onSelect = viewModel::selectStrategy,
                )
            }

            if (s.selectedStrategy != null) {
                Field("Timeframe") {
                    ChipRow(
                        options = s.availableTimeframes,
                        selected = s.timeframe,
                        onSelect = viewModel::selectTimeframe,
                    )
                }
            }

            Field("Instrument") {
                Picker(
                    selected = s.instrument ?: "Select instrument",
                    options = INSTRUMENTS,
                    optionLabel = { it },
                    onSelect = viewModel::selectInstrument,
                )
            }

            // Data availability for the chosen instrument + timeframe.
            when {
                s.loadingRange -> Text("Checking data range…", color = Muted, style = MaterialTheme.typography.bodySmall)
                s.dataRange?.hasData == true -> {
                    val r = s.dataRange!!
                    Text(
                        "Data: ${r.firstTs?.take(10)} → ${r.lastTs?.take(10)}  (${r.nBars} bars)",
                        color = Muted, style = MaterialTheme.typography.bodySmall,
                    )
                }
                s.dataRange != null -> Text(
                    "No data for ${s.instrument} ${s.timeframe} — pick another.",
                    color = Negative, style = MaterialTheme.typography.bodySmall,
                )
            }

            Field("Window") {
                ChipRow(
                    options = WindowPreset.entries.map { it.label },
                    selected = s.window.label,
                    onSelect = { lbl -> WindowPreset.entries.firstOrNull { it.label == lbl }?.let(viewModel::setWindow) },
                )
            }

            Field("Contracts (optional)") {
                OutlinedTextField(
                    value = s.contracts,
                    onValueChange = viewModel::setContracts,
                    singleLine = true,
                    placeholder = { Text("strategy default") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            s.paramSpec?.let { ps ->
                ParamDefaultsSection(ps)
            }

            s.error?.let { Text(it, color = Negative) }

            Button(
                onClick = viewModel::submit,
                enabled = s.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (s.submitting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    Text("Queuing…")
                } else {
                    Text("Run backtest")
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        content()
    }
}

@Composable
private fun <T> Picker(
    selected: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(optionLabel(opt)) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(options: List<String>, selected: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(opt) },
            )
        }
    }
}
