package dev.privatebutts.hrrelay.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import dev.privatebutts.hrrelay.SettingsDataStore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsDataStore: SettingsDataStore,
    onBack: () -> Unit
) {
    val savedUrl by settingsDataStore.serverUrl.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Server URL",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        item {
            Text(
                text = if (savedUrl.isNotBlank()) savedUrl else "(not set)",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                textAlign = TextAlign.Start
            )
        }
        item {
            Text(
                text = "Set URL via adb:\nadb shell am startservice ...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        settingsDataStore.saveServerUrl("")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear URL")
            }
        }
        item {
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
