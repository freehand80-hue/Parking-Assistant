package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.LocationFetchResult
import com.example.data.MapAppOption
import com.example.data.MapLaunchResult
import com.example.data.MapNavigator
import com.example.data.ParkingHistoryItem
import com.example.data.ParkingLocationClient
import com.example.data.ParkingPreferencesRepository
import com.example.data.ParkingReminderReceiver
import com.example.data.ParkingReminderScheduler
import com.example.data.ParkingSpot
import com.example.data.PhotoFileManager
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingPreferencesRepository(application)
    private val locationClient = ParkingLocationClient(application)

    private val _uiState = MutableStateFlow(
        ParkingUiState(
            installedMaps = MapNavigator.getInstalledMapApps(application),
            hasNotificationPermission = ParkingReminderScheduler.hasNotificationPermission(application)
        )
    )
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    init {
        ParkingReminderReceiver.ensureNotificationChannel(application)

        viewModelScope.launch {
            repository.stateFlow.collect { stored ->
                _uiState.update { current ->
                    current.copy(
                        isLoaded = true,
                        activeSpot = stored.activeSpot,
                        preferredMapApp = stored.preferredMapApp,
                        reminderEnabled = stored.reminderEnabled,
                        history = stored.history
                    )
                }
            }
        }

        // Keep elapsed parking timer updated every 30 seconds
        viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                _uiState.update { it.copy(nowMillis = System.currentTimeMillis()) }
            }
        }
    }

    fun hasLocationPermission(): Boolean = locationClient.hasLocationPermission()

    fun hasNotificationPermission(): Boolean =
        ParkingReminderScheduler.hasNotificationPermission(getApplication())

    fun refreshInstalledMaps() {
        val maps = MapNavigator.getInstalledMapApps(getApplication())
        _uiState.update { it.copy(installedMaps = maps) }
    }

    fun refreshNotificationPermission() {
        val granted = ParkingReminderScheduler.hasNotificationPermission(getApplication())
        _uiState.update { it.copy(hasNotificationPermission = granted) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        val app = getApplication<Application>()
        _uiState.update { it.copy(hasNotificationPermission = granted) }
        val spot = _uiState.value.activeSpot
        if (granted && _uiState.value.reminderEnabled && spot != null) {
            val triggerAt = ParkingReminderScheduler.scheduleReminder(
                context = app,
                savedAtMillis = spot.savedAtMillis,
                locationSummary = spot.formattedSummary
            )
            if (triggerAt != null) {
                _uiState.update {
                    it.copy(
                        feedbackBanner = UserFeedbackBanner(
                            message = app.getString(
                                R.string.msg_reminder_scheduled_format,
                                ParkingTimeFormatter.formatClockTime(triggerAt)
                            ),
                            isWarning = false
                        )
                    )
                }
            }
        } else if (!granted) {
            _uiState.update {
                it.copy(
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_notification_permission_denied),
                        isWarning = true
                    )
                )
            }
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            repository.updateReminderEnabled(enabled)
            val spot = _uiState.value.activeSpot
            if (!enabled) {
                ParkingReminderScheduler.cancelReminder(app)
                _uiState.update {
                    it.copy(
                        feedbackBanner = UserFeedbackBanner(
                            message = app.getString(R.string.msg_reminder_cancelled),
                            isWarning = false
                        )
                    )
                }
            } else if (spot != null && ParkingReminderScheduler.hasNotificationPermission(app)) {
                val triggerAt = ParkingReminderScheduler.scheduleReminder(
                    context = app,
                    savedAtMillis = spot.savedAtMillis,
                    locationSummary = spot.formattedSummary
                )
                if (triggerAt != null) {
                    _uiState.update {
                        it.copy(
                            feedbackBanner = UserFeedbackBanner(
                                message = app.getString(
                                    R.string.msg_reminder_scheduled_format,
                                    ParkingTimeFormatter.formatClockTime(triggerAt)
                                ),
                                isWarning = false
                            )
                        )
                    }
                }
            }
        }
    }

    private fun syncReminderForSpot(spot: ParkingSpot?) {
        val app = getApplication<Application>()
        if (spot != null && _uiState.value.reminderEnabled) {
            ParkingReminderScheduler.scheduleReminder(
                context = app,
                savedAtMillis = spot.savedAtMillis,
                locationSummary = spot.formattedSummary
            )
        } else {
            ParkingReminderScheduler.cancelReminder(app)
        }
    }

    fun saveCurrentParkingLocation(refreshGpsOnly: Boolean = false) {
        if (_uiState.value.isLocating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLocating = true, feedbackBanner = null) }
            val result = locationClient.fetchCurrentLocation()
            val now = System.currentTimeMillis()
            val existing = _uiState.value.activeSpot
            val app = getApplication<Application>()

            when (result) {
                is LocationFetchResult.Success -> {
                    val newSpot = if (refreshGpsOnly && existing != null) {
                        existing.copy(
                            latitude = result.latitude,
                            longitude = result.longitude,
                            accuracyMeters = result.accuracyMeters
                        )
                    } else {
                        // Clean up old photo when starting a fresh parking record
                        if (!refreshGpsOnly && existing?.photoPath != null) {
                            PhotoFileManager.deleteFileSafely(existing.photoPath)
                        }
                        ParkingSpot(
                            latitude = result.latitude,
                            longitude = result.longitude,
                            accuracyMeters = result.accuracyMeters,
                            savedAtMillis = now
                        )
                    }
                    repository.saveActiveSpot(newSpot)
                    if (!refreshGpsOnly) {
                        syncReminderForSpot(newSpot)
                    }
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            nowMillis = now,
                            showMemoSheet = !refreshGpsOnly,
                            feedbackBanner = UserFeedbackBanner(
                                message = app.getString(
                                    if (refreshGpsOnly) R.string.msg_gps_updated
                                    else R.string.msg_parking_saved_with_gps
                                ),
                                isWarning = false
                            )
                        )
                    }
                }

                LocationFetchResult.PermissionMissing -> {
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            feedbackBanner = UserFeedbackBanner(
                                message = app.getString(R.string.msg_location_permission_required),
                                isWarning = true
                            )
                        )
                    }
                }

                LocationFetchResult.LocationDisabled -> {
                    if (!refreshGpsOnly && existing == null) {
                        val fallbackSpot = ParkingSpot(
                            latitude = null,
                            longitude = null,
                            accuracyMeters = null,
                            savedAtMillis = now
                        )
                        repository.saveActiveSpot(fallbackSpot)
                        syncReminderForSpot(fallbackSpot)
                    }
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            nowMillis = now,
                            feedbackBanner = UserFeedbackBanner(
                                message = app.getString(R.string.msg_location_disabled),
                                isWarning = true,
                                showLocationSettingsAction = true
                            )
                        )
                    }
                }

                LocationFetchResult.Unavailable -> {
                    if (!refreshGpsOnly) {
                        if (existing?.photoPath != null) {
                            PhotoFileManager.deleteFileSafely(existing.photoPath)
                        }
                        val fallbackSpot = ParkingSpot(
                            latitude = null,
                            longitude = null,
                            accuracyMeters = null,
                            savedAtMillis = now
                        )
                        repository.saveActiveSpot(fallbackSpot)
                        syncReminderForSpot(fallbackSpot)
                    }
                    _uiState.update {
                        it.copy(
                            isLocating = false,
                            nowMillis = now,
                            showMemoSheet = !refreshGpsOnly,
                            feedbackBanner = UserFeedbackBanner(
                                message = app.getString(R.string.msg_parking_saved_memo_only),
                                isWarning = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun onLocationPermissionDenied() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                feedbackBanner = UserFeedbackBanner(
                    message = app.getString(R.string.msg_location_permission_required),
                    isWarning = true
                )
            )
        }
    }

    fun onCameraPermissionDenied() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                feedbackBanner = UserFeedbackBanner(
                    message = app.getString(R.string.msg_camera_permission_required),
                    isWarning = true
                )
            )
        }
    }

    fun onCameraLaunchFailed() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                feedbackBanner = UserFeedbackBanner(
                    message = app.getString(R.string.msg_camera_launch_failed),
                    isWarning = true
                )
            )
        }
    }

    fun saveMemo(floorLabel: String, zoneLabel: String, memo: String) {
        viewModelScope.launch {
            val existing = _uiState.value.activeSpot
            val trimmedFloor = floorLabel.trim()
            val trimmedZone = zoneLabel.trim()
            val trimmedMemo = memo.trim()
            if (existing == null) {
                val createdSpot = ParkingSpot(
                    latitude = null,
                    longitude = null,
                    accuracyMeters = null,
                    savedAtMillis = System.currentTimeMillis(),
                    floorLabel = trimmedFloor,
                    zoneLabel = trimmedZone,
                    memo = trimmedMemo
                )
                repository.saveActiveSpot(createdSpot)
                syncReminderForSpot(createdSpot)
            } else {
                repository.updateMemo(
                    floorLabel = trimmedFloor,
                    zoneLabel = trimmedZone,
                    memo = trimmedMemo
                )
                syncReminderForSpot(
                    existing.copy(
                        floorLabel = trimmedFloor,
                        zoneLabel = trimmedZone,
                        memo = trimmedMemo
                    )
                )
            }
            val app = getApplication<Application>()
            _uiState.update {
                it.copy(
                    showMemoSheet = false,
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_memo_saved),
                        isWarning = false
                    )
                )
            }
        }
    }

    fun onPhotoCaptured(capturedFile: File) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val existing = _uiState.value.activeSpot
            if (existing != null) {
                if (existing.photoPath != null && existing.photoPath != capturedFile.absolutePath) {
                    PhotoFileManager.deleteFileSafely(existing.photoPath)
                }
                repository.updatePhotoPath(capturedFile.absolutePath)
            } else {
                // If user snapped a pillar photo before pressing Save GPS, create an active spot
                // and also attach GPS if permission is already granted.
                val now = System.currentTimeMillis()
                val locResult = if (locationClient.hasLocationPermission()) {
                    locationClient.fetchCurrentLocation()
                } else {
                    LocationFetchResult.PermissionMissing
                }
                val (lat, lng, acc) = when (locResult) {
                    is LocationFetchResult.Success -> Triple(
                        locResult.latitude,
                        locResult.longitude,
                        locResult.accuracyMeters
                    )
                    else -> Triple(null, null, null)
                }
                val createdSpot = ParkingSpot(
                    latitude = lat,
                    longitude = lng,
                    accuracyMeters = acc,
                    savedAtMillis = now,
                    photoPath = capturedFile.absolutePath
                )
                repository.saveActiveSpot(createdSpot)
                syncReminderForSpot(createdSpot)
            }
            _uiState.update {
                it.copy(
                    nowMillis = System.currentTimeMillis(),
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_photo_saved),
                        isWarning = false
                    )
                )
            }
        }
    }

    fun deletePhoto() {
        viewModelScope.launch {
            val existing = _uiState.value.activeSpot ?: return@launch
            PhotoFileManager.deleteFileSafely(existing.photoPath)
            repository.updatePhotoPath(null)
            val app = getApplication<Application>()
            _uiState.update {
                it.copy(
                    showFullScreenPhoto = false,
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_photo_deleted),
                        isWarning = false
                    )
                )
            }
        }
    }

    fun selectMapApp(option: MapAppOption) {
        viewModelScope.launch {
            repository.updatePreferredMapApp(option)
        }
    }

    fun launchWalkingNavigation(context: Context) {
        val app = getApplication<Application>()
        val spot = _uiState.value.activeSpot
        if (spot == null || spot.latitude == null || spot.longitude == null) {
            _uiState.update {
                it.copy(
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_no_gps_for_navigation),
                        isWarning = true
                    )
                )
            }
            return
        }

        val label = spot.formattedSummary.ifBlank { "내 차 주차 위치" }
        val result = MapNavigator.launchWalkingDirections(
            context = context,
            latitude = spot.latitude,
            longitude = spot.longitude,
            destinationLabel = label,
            preferredOption = _uiState.value.preferredMapApp
        )
        handleMapLaunchResult(result)
    }

    fun launchHistoryNavigation(context: Context, item: ParkingHistoryItem) {
        val app = getApplication<Application>()
        if (item.latitude == null || item.longitude == null) {
            _uiState.update {
                it.copy(
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_no_gps_for_navigation),
                        isWarning = true
                    )
                )
            }
            return
        }
        val label = item.formattedSummary.ifBlank { "이전 주차 위치" }
        val result = MapNavigator.launchWalkingDirections(
            context = context,
            latitude = item.latitude,
            longitude = item.longitude,
            destinationLabel = label,
            preferredOption = _uiState.value.preferredMapApp
        )
        _uiState.update { it.copy(showHistorySheet = false) }
        handleMapLaunchResult(result)
    }

    private fun handleMapLaunchResult(result: MapLaunchResult) {
        val app = getApplication<Application>()
        when (result) {
            is MapLaunchResult.Launched -> {
                _uiState.update {
                    it.copy(
                        feedbackBanner = UserFeedbackBanner(
                            message = app.getString(
                                R.string.msg_navigation_launched_format,
                                result.appDisplayName
                            ),
                            isWarning = false
                        )
                    )
                }
            }
            MapLaunchResult.Failed -> {
                _uiState.update {
                    it.copy(
                        feedbackBanner = UserFeedbackBanner(
                            message = app.getString(R.string.msg_no_map_app_found),
                            isWarning = true
                        )
                    )
                }
            }
        }
    }

    fun confirmFinishParking() {
        viewModelScope.launch {
            val existing = _uiState.value.activeSpot
            ParkingReminderScheduler.cancelReminder(getApplication())
            PhotoFileManager.deleteFileSafely(existing?.photoPath)
            repository.archiveAndClearActiveSpot(existing)
            val app = getApplication<Application>()
            _uiState.update {
                it.copy(
                    showFinishConfirmDialog = false,
                    showFullScreenPhoto = false,
                    feedbackBanner = UserFeedbackBanner(
                        message = app.getString(R.string.msg_parking_finished),
                        isWarning = false
                    )
                )
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun showCopiedCoordinatesFeedback() {
        val app = getApplication<Application>()
        _uiState.update {
            it.copy(
                feedbackBanner = UserFeedbackBanner(
                    message = app.getString(R.string.msg_coords_copied),
                    isWarning = false
                )
            )
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(feedbackBanner = null) }
    }

    fun setMemoSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showMemoSheet = visible) }
    }

    fun setHistorySheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showHistorySheet = visible) }
    }

    fun setFullScreenPhotoVisible(visible: Boolean) {
        _uiState.update { it.copy(showFullScreenPhoto = visible) }
    }

    fun setFinishConfirmDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showFinishConfirmDialog = visible) }
    }
}
