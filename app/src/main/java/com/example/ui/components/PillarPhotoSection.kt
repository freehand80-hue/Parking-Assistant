package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import java.io.File

@Composable
fun PillarPhotoSection(
    photoPath: String?,
    onTakePhotoClick: () -> Unit,
    onZoomPhotoClick: () -> Unit,
    onDeletePhotoClick: () -> Unit,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val photoFile = remember(photoPath) {
        photoPath?.let { File(it) }?.takeIf { it.exists() }
    }

    if (photoFile == null) {
        val buttonModifier = if (fillHeight) {
            modifier
                .fillMaxSize()
                .heightIn(min = 56.dp)
        } else {
            modifier
                .fillMaxWidth()
                .height(60.dp)
        }

        Button(
            onClick = onTakePhotoClick,
            modifier = buttonModifier.testTag("take_pillar_photo_button"),
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
    } else {
        Surface(
            modifier = if (fillHeight) modifier.fillMaxSize() else modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
            )
        ) {
            Row(
                modifier = (if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val thumbModifier = if (fillHeight) {
                    Modifier
                        .fillMaxHeight()
                        .heightIn(min = 72.dp)
                        .aspectRatio(1f)
                } else {
                    Modifier.size(72.dp)
                }

                Box(
                    modifier = thumbModifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .clickable(onClick = onZoomPhotoClick)
                        .testTag("saved_pillar_photo_preview")
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoFile)
                            .crossfade(true)
                            .build(),
                        contentDescription = stringResource(R.string.cd_saved_pillar_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = stringResource(R.string.action_zoom_photo),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(15.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.label_pillar_photo_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "터치해 확대",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTakePhotoClick,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("retake_pillar_photo_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = stringResource(R.string.action_retake_photo),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = onDeletePhotoClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("delete_pillar_photo_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "삭제",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoViewerDialog(
    photoPath: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler(onBack = onDismiss)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040A14))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = nextScale
                        offset = if (nextScale > 1f) {
                            offset + pan
                        } else {
                            Offset.Zero
                        }
                    }
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(photoPath))
                    .build(),
                contentDescription = stringResource(R.string.cd_saved_pillar_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.photo_viewer_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = stringResource(R.string.photo_viewer_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("close_photo_viewer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_dialog),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
