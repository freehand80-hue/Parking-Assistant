package com.example.data

import java.util.Locale

enum class MapAppOption(val storageKey: String) {
    AUTO("auto"),
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google");

    companion object {
        fun fromKey(key: String?): MapAppOption {
            return entries.firstOrNull { it.storageKey == key } ?: AUTO
        }
    }
}

data class InstalledMapApps(
    val hasKakaoMap: Boolean = false,
    val hasNaverMap: Boolean = false,
    val hasGoogleMaps: Boolean = false
)

data class ParkingSpot(
    val latitude: Double?,
    val longitude: Double?,
    val accuracyMeters: Float?,
    val savedAtMillis: Long,
    val floorLabel: String = "",
    val zoneLabel: String = "",
    val memo: String = "",
    val photoPath: String? = null
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    val formattedSummary: String
        get() = buildList {
            if (floorLabel.isNotBlank()) add(floorLabel.trim())
            if (zoneLabel.isNotBlank()) add(zoneLabel.trim())
            if (memo.isNotBlank()) add(memo.trim())
        }.joinToString(" · ")

    val formattedCoordinates: String
        get() {
            val lat = latitude ?: return ""
            val lng = longitude ?: return ""
            return String.format(Locale.US, "%.6f, %.6f", lat, lng)
        }
}

data class ParkingHistoryItem(
    val id: Long,
    val latitude: Double?,
    val longitude: Double?,
    val savedAtMillis: Long,
    val floorLabel: String,
    val zoneLabel: String,
    val memo: String
) {
    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null

    val formattedSummary: String
        get() = buildList {
            if (floorLabel.isNotBlank()) add(floorLabel.trim())
            if (zoneLabel.isNotBlank()) add(zoneLabel.trim())
            if (memo.isNotBlank()) add(memo.trim())
        }.joinToString(" · ")
}
