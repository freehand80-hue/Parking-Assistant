package com.example.ui

import com.example.data.InstalledMapApps
import com.example.data.MapAppOption
import com.example.data.ParkingHistoryItem
import com.example.data.ParkingSpot

data class UserFeedbackBanner(
    val message: String,
    val isWarning: Boolean = false,
    val showLocationSettingsAction: Boolean = false
)

data class ParkingUiState(
    val isLoaded: Boolean = false,
    val isLocating: Boolean = false,
    val activeSpot: ParkingSpot? = null,
    val history: List<ParkingHistoryItem> = emptyList(),
    val preferredMapApp: MapAppOption = MapAppOption.AUTO,
    val installedMaps: InstalledMapApps = InstalledMapApps(),
    val reminderEnabled: Boolean = true,
    val hasNotificationPermission: Boolean = true,
    val nowMillis: Long = System.currentTimeMillis(),
    val feedbackBanner: UserFeedbackBanner? = null,
    val showMemoSheet: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showFullScreenPhoto: Boolean = false,
    val showFinishConfirmDialog: Boolean = false
) {
    val elapsedText: String
        get() = activeSpot?.let {
            ParkingTimeFormatter.formatElapsedDuration(it.savedAtMillis, nowMillis)
        }.orEmpty()
}
