package com.programovil.aura

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.programovil.aura.notification.NotificationHelper

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "Test Notification"
        val body = remoteMessage.notification?.body ?: "This is a test push notification"

        NotificationHelper.createNotificationChannels(this)
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        // Optionally: send token to your backend for targeted notifications
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager

        val notification = androidx.core.app.NotificationCompat.Builder(this, NotificationHelper.CHANNEL_DAILY_SUMMARY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}