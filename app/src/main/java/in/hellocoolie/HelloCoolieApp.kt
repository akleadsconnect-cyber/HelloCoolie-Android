package in.hellocoolie

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HelloCoolieApp : Application() {

    companion object {
        const val CHANNEL_BOOKINGS = "bookings"
        const val CHANNEL_ALERTS   = "alerts"
        const val CHANNEL_WALLET   = "wallet"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)

        listOf(
            NotificationChannel(CHANNEL_BOOKINGS, "Booking Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "New booking requests and status updates"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            },
            NotificationChannel(CHANNEL_ALERTS, "SOS & Fraud Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Emergency and fraud alerts"
            },
            NotificationChannel(CHANNEL_WALLET, "Wallet Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Payment and withdrawal notifications"
            }
        ).forEach { manager.createNotificationChannel(it) }
    }
}
