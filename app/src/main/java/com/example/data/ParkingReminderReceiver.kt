package com.example.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class ParkingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PARKING_FEE_REMINDER) return

        val locationSummary = intent.getStringExtra(EXTRA_LOCATION_SUMMARY).orEmpty()
        showReminderNotification(context, locationSummary)
    }

    companion object {
        const val ACTION_PARKING_FEE_REMINDER = "com.example.action.PARKING_FEE_REMINDER"
        const val EXTRA_LOCATION_SUMMARY = "extra_location_summary"
        const val CHANNEL_ID = "parking_fee_reminder_channel"
        const val NOTIFICATION_ID = 2001

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        context.getString(R.string.notification_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.notification_channel_desc)
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun showReminderNotification(context: Context, locationSummary: String) {
            ensureNotificationChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val contentText = if (locationSummary.isNotBlank()) {
                context.getString(R.string.notification_fee_body_with_memo, locationSummary)
            } else {
                context.getString(R.string.notification_fee_body_default)
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_parking)
                .setContentTitle(context.getString(R.string.notification_fee_title))
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(contentPendingIntent)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Notification permission revoked by user
            }
        }
    }
}
