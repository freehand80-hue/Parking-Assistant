package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ParkingTimeFormatter {

    fun formatSavedTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA)
        return formatter.format(Date(millis))
    }

    fun formatCompactTimestamp(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREA)
        return formatter.format(Date(millis))
    }

    fun formatElapsedDuration(savedAtMillis: Long, nowMillis: Long): String {
        val diffMillis = (nowMillis - savedAtMillis).coerceAtLeast(0L)
        val totalMinutes = diffMillis / 60_000L
        return when {
            totalMinutes < 1L -> "방금 주차함"
            totalMinutes < 60L -> "${totalMinutes}분 경과"
            totalMinutes < 24 * 60L -> {
                val hours = totalMinutes / 60L
                val minutes = totalMinutes % 60L
                if (minutes == 0L) "${hours}시간 경과" else "${hours}시간 ${minutes}분 경과"
            }
            else -> {
                val days = totalMinutes / (24 * 60L)
                val hours = (totalMinutes % (24 * 60L)) / 60L
                if (hours == 0L) "${days}일 경과" else "${days}일 ${hours}시간 경과"
            }
        }
    }

    fun formatClockTime(millis: Long): String {
        val formatter = SimpleDateFormat("a h:mm", Locale.KOREA)
        return formatter.format(Date(millis))
    }

    fun formatRemainingDuration(targetMillis: Long, nowMillis: Long): String {
        val remainingMillis = (targetMillis - nowMillis).coerceAtLeast(0L)
        if (remainingMillis < 60_000L) return "곧 알림 예정"
        val totalMinutes = (remainingMillis + 59_999L) / 60_000L
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return when {
            hours == 0L -> "${minutes}분 남음"
            minutes == 0L -> "${hours}시간 남음"
            else -> "${hours}시간 ${minutes}분 남음"
        }
    }
}
