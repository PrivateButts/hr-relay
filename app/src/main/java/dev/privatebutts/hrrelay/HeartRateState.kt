package dev.privatebutts.hrrelay

import kotlinx.coroutines.flow.MutableStateFlow

object HeartRateState {
    val latestBpm = MutableStateFlow<Int?>(null)
    val error = MutableStateFlow<String?>(null)
    val isRunning = MutableStateFlow(false)
}
