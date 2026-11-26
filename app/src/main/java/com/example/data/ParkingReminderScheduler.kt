package com.example.data

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object ParkingReminderScheduler {

    const val REMINDER_DELAY_MILLIS = 2L * 60L * 60L * 1000L // 2 hours
    private const val ALARM_REQUEST_CODE = 2002

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Schedules a 2-hour parking fee settlement reminder starting from [savedAtMillis].
     * Returns the scheduled trigger timestamp in epoch millis, or null if the trigger time
     * has already passed or scheduling was unavailable.
     */
    fun scheduleReminder(
        context: Context,
        savedAtMillis: Long,
        locationSummary: String
    ): Long? {
        ParkingReminderReceiver.ensureNotificationChannel(context)

        val triggerAtMillis = savedAtMillis + REMINDER_DELAY_MILLIS
        if (triggerAtMillis <= System.currentTimeMillis()) {
            cancelReminder(context)
            return null
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return null

        val pendingIntent = buildReminderPendingIntent(context, locationSummary)

        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            return triggerAtMillis
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            return triggerAtMillis
        } catch (_: Exception) {
            return null
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, ParkingReminderReceiver::class.java).apply {
                action = ParkingReminderReceiver.ACTION_PARKING_FEE_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        NotificationManagerCompat.from(context).cancel(ParkingReminderReceiver.NOTIFICATION_ID)
    }

    private fun buildReminderPendingIntent(
        context: Context,
        locationSummary: String
    ): PendingIntent {
        val intent = Intent(context, ParkingReminderReceiver::class.java).apply {
            action = ParkingReminderReceiver.ACTION_PARKING_FEE_REMINDER
            putExtra(ParkingReminderReceiver.EXTRA_LOCATION_SUMMARY, locationSummary)
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
