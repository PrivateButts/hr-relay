package dev.privatebutts.hrrelay

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HeartRateService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var hrCollectionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var healthServicesManager: HealthServicesManager
    private lateinit var heartRateClient: HeartRateClient
    private lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate() {
        super.onCreate()
        healthServicesManager = HealthServicesManager(this)
        heartRateClient = HeartRateClient()
        settingsDataStore = SettingsDataStore(this)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "hrrelay:wakelock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")

        wakeLock?.acquire(60 * 60 * 1000L)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, HrRelayApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(HrRelayApp.NOTIFICATION_ID, notification)

        HeartRateState.isRunning.value = true

        hrCollectionJob?.cancel()
        hrCollectionJob = serviceScope.launch {
            var serverUrl = ""
            var beatCount = 0
            launch {
                settingsDataStore.serverUrl.collect { serverUrl = it }
            }
            healthServicesManager.heartRateMeasureFlow()
                .catch { e ->
                    Log.e(TAG, "Flow error", e)
                    HeartRateState.error.value =
                        "HR error: ${e.localizedMessage ?: "unknown"}"
                }
                .collect { message ->
                    when (message) {
                        is MeasureMessage.MeasureData -> {
                            beatCount++
                            HeartRateState.latestBpm.value = message.bpm.toInt()
                            HeartRateState.error.value = null
                            Log.d(TAG, "Beat #$beatCount bpm=${message.bpm.toInt()}")

                            if (serverUrl.isNotBlank()) {
                                try {
                                    withContext(Dispatchers.IO) {
                                        heartRateClient.send(serverUrl, message.bpm)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Send failed", e)
                                    HeartRateState.error.value =
                                        "Send: ${e.localizedMessage ?: "failed"}"
                                }
                            }
                        }
                        is MeasureMessage.MeasureAvailability -> {
                            Log.d(TAG, "Availability: ${message.availability}")
                        }
                        is MeasureMessage.MeasureError -> {
                            Log.e(TAG, "Measure error", message.error)
                            HeartRateState.error.value =
                                message.error.localizedMessage ?: "Sensor access denied"
                        }
                    }
                }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceScope.cancel()
        HeartRateState.latestBpm.value = null
        HeartRateState.error.value = null
        HeartRateState.isRunning.value = false
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved, restarting")
        val restartIntent = Intent(this, HeartRateService::class.java)
        startService(restartIntent)
    }

    companion object {
        private const val TAG = "HeartRateService"
    }
}
