package com.rx.geminipro.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue

data class NotificationData(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val key: String
)

class GeminiNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "GeminiNotifListener"
        private const val MAX_NOTIFICATIONS = 50
        var instance: GeminiNotificationListener? = null
            private set
        val notifications = ConcurrentLinkedQueue<NotificationData>()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Notification Listener created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val packageName = sbn.packageName ?: ""

        // Skip our own notifications
        if (packageName == applicationContext.packageName) return

        val notifData = NotificationData(
            packageName = packageName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            key = sbn.key
        )

        notifications.add(notifData)

        // Keep queue size limited
        while (notifications.size > MAX_NOTIFICATIONS) {
            notifications.poll()
        }

        Log.d(TAG, "Notification from $packageName: $title - $text")

        // Log the notification
        ActionLogger.getInstance()?.logAction(
            "notification_captured",
            "From: $packageName | $title: $text",
            "logged"
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        notifications.removeAll { it.key == sbn.key }
    }

    // Get all current notifications as formatted text
    fun getNotificationSummary(): String {
        if (notifications.isEmpty()) return "No recent notifications."

        val builder = StringBuilder("Recent Notifications:\n")
        notifications.takeLast(10).forEach { notif ->
            builder.append("• ${notif.packageName}: ${notif.title} - ${notif.text}\n")
        }
        return builder.toString()
    }

    // Get notifications from specific app
    fun getNotificationsFromApp(packageName: String): List<NotificationData> {
        return notifications.filter { it.packageName == packageName }
    }

    // Clear all stored notifications
    fun clearStoredNotifications() {
        notifications.clear()
    }

    // Dismiss a notification
    fun dismissNotification(key: String) {
        cancelNotification(key)
    }

    // Dismiss all notifications
    fun dismissAllNotifications() {
        cancelAllNotifications()
    }
}
