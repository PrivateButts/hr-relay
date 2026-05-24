package dev.privatebutts.hrrelay

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class HrRelayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_desc)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "hr_relay"
        const val NOTIFICATION_ID = 1
    }
}
