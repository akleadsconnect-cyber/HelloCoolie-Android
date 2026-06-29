package in.hellocoolie.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import in.hellocoolie.HelloCoolieApp
import in.hellocoolie.R
import in.hellocoolie.data.api.HelloCoolieApi
import in.hellocoolie.ui.porter.PorterMainActivity
import in.hellocoolie.ui.user.UserMainActivity
import in.hellocoolie.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HelloCoolieFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var api: HelloCoolieApi
    @Inject lateinit var tokenManager: TokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update FCM token on server
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateFcmToken(mapOf("fcm_token" to token))
            } catch (e: Exception) { /* ignore */ }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data  = message.data
        val notif = message.notification

        val type    = data["type"] ?: ""
        val title   = notif?.title ?: data["title"] ?: "HelloCoolie"
        val body    = notif?.body  ?: data["body"]  ?: ""

        val channel = when (type) {
            "new_request", "booking_update" -> HelloCoolieApp.CHANNEL_BOOKINGS
            "sos", "fraud"                  -> HelloCoolieApp.CHANNEL_ALERTS
            "wallet", "payment"             -> HelloCoolieApp.CHANNEL_WALLET
            else                            -> HelloCoolieApp.CHANNEL_BOOKINGS
        }

        val role = tokenManager.getRole()
        val activityClass = if (role == "porter") PorterMainActivity::class.java else UserMainActivity::class.java

        val intent = Intent(this, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            data?.forEach { (k, v) -> putExtra(k, v) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(
                if (channel == HelloCoolieApp.CHANNEL_BOOKINGS)
                    NotificationCompat.PRIORITY_MAX
                else
                    NotificationCompat.PRIORITY_DEFAULT
            )
            .setContentIntent(pendingIntent)
            .also {
                if (type == "new_request") {
                    it.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                    it.setDefaults(NotificationCompat.DEFAULT_SOUND)
                }
            }
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
