package dev.privatebutts.hrrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SetUrlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("url") ?: return
        Log.d(TAG, "Received SET_URL broadcast: $url")
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                SettingsDataStore(context.applicationContext).saveServerUrl(url)
                Log.d(TAG, "URL saved successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save URL", e)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SetUrlReceiver"
    }
}
