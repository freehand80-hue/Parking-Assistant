package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.PhotoFileManager
import com.example.ui.components.FindMyCarNavigationCard
import com.example.ui.components.FullScreenPhotoViewerDialog
import com.example.ui.components.MemoEditorSheet
import com.example.ui.components.ParkedSummaryCard
import com.example.ui.components.ParkingHistorySheet
import com.example.ui.components.ParkingReminderCard
import com.example.ui.components.PillarPhotoSection
import com.example.ui.components.parkingNavyBackdrop
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun ParkingScreen(
    viewModel: ParkingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingRefreshGpsOnly by rememberSaveable { mutableStateOf(false) }
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshInstalledMaps()
        viewModel.refreshNotificationPermission()
    }

    // Auto-dismiss non-actionable feedback toasts after 4 seconds so the screen stays clean
    LaunchedEffect(uiState.feedbackBanner) {
        val banner = uiState.feedbackBanner
        if (banner != null && !banner.showLocationSettingsAction) {
            delay(4_000L)
            viewModel.dismissBanner()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !viewModel.hasNotificationPermission()
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            permissions.containsKey(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            viewModel.refreshNotificationPermission()
        }
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.saveCurrentParkingLocation(refreshGpsOnly = pendingRefreshGpsOnly)
        } else {
            viewModel.onLocationPermissionDenied()
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (success && path != null) {
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                viewModel.onPhotoCaptured(file)
            } else {
                PhotoFileManager.deleteFileSafely(path)
            }
        } else if (path != null) {
            PhotoFileManager.deleteFileSafely(path)
        }
    }

    fun launchCameraCapture() {
        try {
            val capture = PhotoFileManager.createCaptureFile(context)
            pendingPhotoPath = capture.file.absolutePath
            takePictureLauncher.launch(capture.contentUri)
        } catch (_: Exception) {
            pendingPhotoPath = null
            viewModel.onCameraLaunchFailed()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            viewModel.onCameraPermissionDenied()
        }
    }

    fun requestSaveGps(refreshGpsOnly: Boolean) {
        pendingRefreshGpsOnly = refreshGpsOnly
        val needsNotificationPerm = !refreshGpsOnly &&
            uiState.reminderEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !viewModel.hasNotificationPermission()

        if (viewModel.hasLocationPermission()) {
            if (needsNotificationPerm) {
                requestNotificationPermissionIfNeeded()
            }
            viewModel.saveCurrentParkingLocation(refreshGpsOnly = refreshGpsOnly)
        } else {
            val requested = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                if (needsNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            locationPermissionLauncher.launch(requested.toTypedArray())
        }
    }

    fun handleToggleReminder(enabled: Boolean) {
        viewModel.setReminderEnabled(enabled)
        if (enabled) {
            requestNotificationPermissionIfNeeded()
        }
    }

    fun requestTakePhoto() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCameraPermission) {
            launchCameraCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .parkingNavyBackdrop(
                    baseColor = MaterialTheme.colorScheme.background,
                    accentGlow = MaterialTheme.colorScheme.primary,
                    gridLineColor = MaterialTheme.colorScheme.outline
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWideScreen = maxWidth >= 720.dp
                // Fill the entire viewport proportionally without scrolling on standard phones
                val useFillViewport = !isWideScreen && maxHeight >= 600.dp
                val contentMaxWidth = if (isWideScreen) 960.dp else 600.dp

                val rootColumnModifier = if (useFillViewport) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                } else {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                }

                Column(
                    modifier = rootColumnModifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = if (useFillViewport) {
                            Modifier
                                .fillMaxSize()
                                .widthIn(max = contentMaxWidth)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = contentMaxWidth)
                        },
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ParkingTopBar(
                            isParked = uiState.activeSpot != null,
                            historyCount = uiState.history.size,
                            onHistoryClick = { viewModel.setHistorySheetVisible(true) }
                        )

                        val bodyModifier = if (useFillViewport) {
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        } else {
                            Modifier.fillMaxWidth()
                        }

                        val activeSpot = uiState.activeSpot
                        if (activeSpot == null) {
                            ReadyToParkContent(
                                isWideScreen = isWideScreen,
                                fillViewport = useFillViewport,
                                isLocating = uiState.isLocating,
                                reminderEnabled = uiState.reminderEnabled,
                                hasNotificationPermission = uiState.hasNotificationPermission,
                                nowMillis = uiState.nowMillis,
                                onToggleReminder = { handleToggleReminder(it) },
                                onRequestNotificationPermission = {
                                    requestNotificationPermissionIfNeeded()
                                },
                                onSaveParkingClick = { requestSaveGps(refreshGpsOnly = false) },
                                onTakePillarPhotoClick = { requestTakePhoto() },
                                onMemoOnlyClick = { viewModel.setMemoSheetVisible(true) },
                                modifier = bodyModifier
                            )
                        } else {
                            ParkedModeContent(
                                isWideScreen = isWideScreen,
                                fillViewport = useFillViewport,
                                spot = activeSpot,
                                elapsedText = uiState.elapsedText,
                                isLocating = uiState.isLocating,
                                selectedMapApp = uiState.preferredMapApp,
                                installedMaps = uiState.installedMaps,
                                reminderEnabled = uiState.reminderEnabled,
                                hasNotificationPermission = uiState.hasNotificationPermission,
                                nowMillis = uiState.nowMillis,
                                onToggleReminder = { handleToggleReminder(it) },
                                onRequestNotificationPermission = {
                                    requestNotificationPermissionIfNeeded()
                                },
                                onEditMemoClick = { viewModel.setMemoSheetVisible(true) },
                                onCopyCoordsClick = {
                                    copyToClipboard(context, activeSpot.formattedCoordinates)
                                    viewModel.showCopiedCoordinatesFeedback()
                                },
                                onRefreshGpsClick = { requestSaveGps(refreshGpsOnly = true) },
                                onSelectMapApp = { viewModel.selectMapApp(it) },
                                onFindMyCarClick = { viewModel.launchWalkingNavigation(context) },
                                onTakePhotoClick = { requestTakePhoto() },
                                onZoomPhotoClick = { viewModel.setFullScreenPhotoVisible(true) },
                                onDeletePhotoClick = { viewModel.deletePhoto() },
                                onResaveSpotClick = { requestSaveGps(refreshGpsOnly = false) },
                                onFinishParkingClick = {
                                    viewModel.setFinishConfirmDialogVisible(true)
                                },
                                modifier = bodyModifier
                            )
                        }
                    }
                }

                // Floating high-contrast feedback banner that never pushes or shrinks buttons
                AnimatedVisibility(
                    visible = uiState.feedbackBanner != null,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 64.dp)
                        .widthIn(max = contentMaxWidth)
                ) {
                    uiState.feedbackBanner?.let { banner ->
                        FeedbackBannerCard(
                            banner = banner,
                            onDismiss = { viewModel.dismissBanner() },
                            onOpenLocationSettings = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    )
                                } catch (_: Exception) {
                                    // Ignore if unavailable
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showMemoSheet) {
        val spot = uiState.activeSpot
        MemoEditorSheet(
            initialFloor = spot?.floorLabel.orEmpty(),
            initialZone = spot?.zoneLabel.orEmpty(),
            initialMemo = spot?.memo.orEmpty(),
            onDismiss = { viewModel.setMemoSheetVisible(false) },
            onSave = { floor, zone, memo ->
                viewModel.saveMemo(floor, zone, memo)
            }
        )
    }

    if (uiState.showHistorySheet) {
        ParkingHistorySheet(
            history = uiState.history,
            onDismiss = { viewModel.setHistorySheetVisible(false) },
            onNavigateToItem = { item ->
                viewModel.launchHistoryNavigation(context, item)
            },
            onClearAll = { viewModel.clearHistory() }
        )
    }

    if (uiState.showFullScreenPhoto) {
        val photoPath = uiState.activeSpot?.photoPath
        if (!photoPath.isNullOrBlank()) {
            FullScreenPhotoViewerDialog(
                photoPath = photoPath,
                onDismiss = { viewModel.setFullScreenPhotoVisible(false) }
            )
        }
    }

    if (uiState.showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setFinishConfirmDialogVisible(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = {
                Text(
                    text = stringResource(R.string.dialog_finish_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_finish_body),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.setFinishConfirmDialogVisible(false) },
                    modifier = Modifier.testTag("cancel_finish_parking_button")
                ) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmFinishParking() },
                    modifier = Modifier.testTag("confirm_finish_parking_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.dialog_confirm_finish),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )
    }
}

@Composable
private fun ParkingTopBar(
    isParked: Boolean,
    historyCount: Int,
    onHistoryClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isParked) {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isParked) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isParked) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                    )
                    Text(
                        text = stringResource(
                            if (isParked) R.string.status_parked_badge
                            else R.string.status_ready_badge
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isParked) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        Surface(
            onClick = onHistoryClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("history_icon_button"),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = stringResource(R.string.cd_history_button),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                if (historyCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = historyCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyToParkContent(
    isWideScreen: Boolean,
    fillViewport: Boolean,
    isLocating: Boolean,
    reminderEnabled: Boolean,
    hasNotificationPermission: Boolean,
    nowMillis: Long,
    onToggleReminder: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSaveParkingClick: () -> Unit,
    onTakePillarPhotoClick: () -> Unit,
    onMemoOnlyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Composable
    fun HeroBannerCard(cardModifier: Modifier) {
        Surface(
            modifier = cardModifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_parking_hero_1795628579429),
                    contentDescription = stringResource(R.string.cd_parking_hero_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ready_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }

    @Composable
    fun SaveGpsButton(btnModifier: Modifier) {
        Button(
            onClick = onSaveParkingClick,
            enabled = !isLocating,
            modifier = btnModifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .testTag("save_parking_location_button"),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 5.dp)
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.size(14.dp))
                Text(
                    text = stringResource(R.string.action_saving_gps),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.AddLocationAlt,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.size(14.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.action_save_parking_location),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.action_save_parking_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }

    @Composable
    fun TakePhotoButton(btnModifier: Modifier) {
        Button(
            onClick = onTakePillarPhotoClick,
            modifier = btnModifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag("ready_take_pillar_photo_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = stringResource(R.string.action_take_pillar_photo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(R.string.action_take_pillar_photo_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }

    @Composable
    fun MemoOnlyButton(btnModifier: Modifier) {
        OutlinedButton(
            onClick = onMemoOnlyClick,
            modifier = btnModifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("ready_memo_only_button"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f))
        ) {
            Icon(
                imageVector = Icons.Filled.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.action_edit_memo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }

    if (isWideScreen) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            HeroBannerCard(
                cardModifier = Modifier
                    .weight(1f)
                    .height(250.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SaveGpsButton(Modifier.height(74.dp))
                TakePhotoButton(Modifier.height(62.dp))
                MemoOnlyButton(Modifier.height(52.dp))
                ParkingReminderCard(
                    reminderEnabled = reminderEnabled,
                    hasNotificationPermission = hasNotificationPermission,
                    savedAtMillis = null,
                    nowMillis = nowMillis,
                    onToggleReminder = onToggleReminder,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
            }
        }
    } else if (fillViewport) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeroBannerCard(Modifier.weight(1.85f))
            SaveGpsButton(Modifier.weight(1.2f))
            TakePhotoButton(Modifier.weight(0.92f))
            MemoOnlyButton(Modifier.weight(0.72f))
            ParkingReminderCard(
                reminderEnabled = reminderEnabled,
                hasNotificationPermission = hasNotificationPermission,
                savedAtMillis = null,
                nowMillis = nowMillis,
                onToggleReminder = onToggleReminder,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeroBannerCard(Modifier.height(148.dp))
            SaveGpsButton(Modifier.height(70.dp))
            TakePhotoButton(Modifier.height(58.dp))
            MemoOnlyButton(Modifier.height(50.dp))
            ParkingReminderCard(
                reminderEnabled = reminderEnabled,
                hasNotificationPermission = hasNotificationPermission,
                savedAtMillis = null,
                nowMillis = nowMillis,
                onToggleReminder = onToggleReminder,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
        }
    }
}

@Composable
private fun ParkedModeContent(
    isWideScreen: Boolean,
    fillViewport: Boolean,
    spot: com.example.data.ParkingSpot,
    elapsedText: String,
    isLocating: Boolean,
    selectedMapApp: com.example.data.MapAppOption,
    installedMaps: com.example.data.InstalledMapApps,
    reminderEnabled: Boolean,
    hasNotificationPermission: Boolean,
    nowMillis: Long,
    onToggleReminder: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onEditMemoClick: () -> Unit,
    onCopyCoordsClick: () -> Unit,
    onRefreshGpsClick: () -> Unit,
    onSelectMapApp: (com.example.data.MapAppOption) -> Unit,
    onFindMyCarClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onZoomPhotoClick: () -> Unit,
    onDeletePhotoClick: () -> Unit,
    onResaveSpotClick: () -> Unit,
    onFinishParkingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    @Composable
    fun BottomActionRow(rowModifier: Modifier = Modifier) {
        Row(
            modifier = rowModifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onResaveSpotClick,
                enabled = !isLocating,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("resave_parking_location_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.action_resave_parking_location),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            OutlinedButton(
                onClick = onFinishParkingClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("finish_parking_button"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.action_finish_parking),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    if (isWideScreen) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParkedSummaryCard(
                    spot = spot,
                    elapsedText = elapsedText,
                    isLocating = isLocating,
                    onEditMemoClick = onEditMemoClick,
                    onCopyCoordsClick = onCopyCoordsClick,
                    onRefreshGpsClick = onRefreshGpsClick
                )
                FindMyCarNavigationCard(
                    hasCoordinates = spot.hasCoordinates,
                    selectedMapApp = selectedMapApp,
                    installedMaps = installedMaps,
                    onSelectMapApp = onSelectMapApp,
                    onNavigateClick = onFindMyCarClick
                )
            }
            Column(
                modifier = Modifier.weight(0.9f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParkingReminderCard(
                    reminderEnabled = reminderEnabled,
                    hasNotificationPermission = hasNotificationPermission,
                    savedAtMillis = spot.savedAtMillis,
                    nowMillis = nowMillis,
                    onToggleReminder = onToggleReminder,
                    onRequestNotificationPermission = onRequestNotificationPermission
                )
                PillarPhotoSection(
                    photoPath = spot.photoPath,
                    onTakePhotoClick = onTakePhotoClick,
                    onZoomPhotoClick = onZoomPhotoClick,
                    onDeletePhotoClick = onDeletePhotoClick
                )
                BottomActionRow()
            }
        }
    } else if (fillViewport) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParkedSummaryCard(
                spot = spot,
                elapsedText = elapsedText,
                isLocating = isLocating,
                onEditMemoClick = onEditMemoClick,
                onCopyCoordsClick = onCopyCoordsClick,
                onRefreshGpsClick = onRefreshGpsClick
            )

            FindMyCarNavigationCard(
                hasCoordinates = spot.hasCoordinates,
                selectedMapApp = selectedMapApp,
                installedMaps = installedMaps,
                onSelectMapApp = onSelectMapApp,
                onNavigateClick = onFindMyCarClick,
                fillHeight = true,
                modifier = Modifier.weight(1.35f)
            )

            ParkingReminderCard(
                reminderEnabled = reminderEnabled,
                hasNotificationPermission = hasNotificationPermission,
                savedAtMillis = spot.savedAtMillis,
                nowMillis = nowMillis,
                onToggleReminder = onToggleReminder,
                onRequestNotificationPermission = onRequestNotificationPermission
            )

            PillarPhotoSection(
                photoPath = spot.photoPath,
                onTakePhotoClick = onTakePhotoClick,
                onZoomPhotoClick = onZoomPhotoClick,
                onDeletePhotoClick = onDeletePhotoClick,
                fillHeight = true,
                modifier = Modifier.weight(0.95f)
            )

            BottomActionRow()
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParkedSummaryCard(
                spot = spot,
                elapsedText = elapsedText,
                isLocating = isLocating,
                onEditMemoClick = onEditMemoClick,
                onCopyCoordsClick = onCopyCoordsClick,
                onRefreshGpsClick = onRefreshGpsClick
            )
            FindMyCarNavigationCard(
                hasCoordinates = spot.hasCoordinates,
                selectedMapApp = selectedMapApp,
                installedMaps = installedMaps,
                onSelectMapApp = onSelectMapApp,
                onNavigateClick = onFindMyCarClick
            )
            ParkingReminderCard(
                reminderEnabled = reminderEnabled,
                hasNotificationPermission = hasNotificationPermission,
                savedAtMillis = spot.savedAtMillis,
                nowMillis = nowMillis,
                onToggleReminder = onToggleReminder,
                onRequestNotificationPermission = onRequestNotificationPermission
            )
            PillarPhotoSection(
                photoPath = spot.photoPath,
                onTakePhotoClick = onTakePhotoClick,
                onZoomPhotoClick = onZoomPhotoClick,
                onDeletePhotoClick = onDeletePhotoClick
            )
            BottomActionRow()
        }
    }
}

@Composable
private fun FeedbackBannerCard(
    banner: UserFeedbackBanner,
    onDismiss: () -> Unit,
    onOpenLocationSettings: () -> Unit
) {
    val containerColor = if (banner.isWarning) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val accentColor = if (banner.isWarning) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feedback_banner"),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 8.dp,
        border = BorderStroke(1.5.dp, accentColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (banner.isWarning) {
                        Icons.Filled.WarningAmber
                    } else {
                        Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = banner.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_dialog),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (banner.showLocationSettingsAction) {
                Button(
                    onClick = onOpenLocationSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_location_settings_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.action_open_location_settings),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("GPS Coordinates", text))
}
