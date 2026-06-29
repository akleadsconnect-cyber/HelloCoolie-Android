package com.hellocoolie.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hellocoolie.HelloCoolieApp
import com.hellocoolie.R
import com.hellocoolie.ui.user.UserMainActivity
import com.hellocoolie.utils.TokenManager
import javax.inject.Inject

// FCM Service — requires google-services.json to activate
// Add google-services.json to /app/ and uncomment manifest entry to enable push notifications
/*
@dagger.hilt.android.AndroidEntryPoint
class HelloCoolieFirebaseService : com.google.firebase.messaging.FirebaseMessagingService() {

    @Inject lateinit var tokenManager: TokenManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Update FCM token on server when Firebase is configured
    }

    override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "HelloCoolie"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, UserMainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(this, HelloCoolieApp.CHANNEL_BOOKINGS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }
}
*/

// Placeholder until google-services.json is added
class HelloCoolieFirebaseService
