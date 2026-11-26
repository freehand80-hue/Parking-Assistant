package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ParkingSpot
import com.example.ui.ParkingTimeFormatter
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

@Composable
fun ParkedSummaryCard(
    spot: ParkingSpot,
    elapsedText: String,
    isLocating: Boolean,
    onEditMemoClick: () -> Unit,
    onCopyCoordsClick: () -> Unit,
    onRefreshGpsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Saved timestamp + Live elapsed badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalParking,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = ParkingTimeFormatter.formatSavedTimestamp(spot.savedAtMillis),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.size(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = elapsedText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Row 2: Floor / Zone / Memo interactive block
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditMemoClick)
                    .testTag("edit_memo_card_button"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (spot.formattedSummary.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (spot.formattedSummary.isNotBlank()) {
                            Text(
                                text = spot.formattedSummary,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.label_no_memo_yet),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.label_tap_to_add_memo),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "수정",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )

            // Row 3: GPS Coordinates + inline Copy / Refresh buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (spot.hasCoordinates) {
                                Icons.Filled.GpsFixed
                            } else {
                                Icons.Filled.GpsOff
                            },
                            contentDescription = null,
                            tint = if (spot.hasCoordinates) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (spot.hasCoordinates) {
                                stringResource(R.string.label_gps_coords)
                            } else {
                                stringResource(R.string.label_gps_not_captured)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (spot.hasCoordinates) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        spot.accuracyMeters?.let { accuracy ->
                            Text(
                                text = "±${accuracy.roundToInt().coerceAtLeast(1)}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    if (spot.hasCoordinates) {
                        Text(
                            text = spot.formattedCoordinates,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("saved_gps_coordinates_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (spot.hasCoordinates) {
                        OutlinedButton(
                            onClick = onCopyCoordsClick,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("copy_coords_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.action_copy_coords),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "복사",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onRefreshGpsClick,
                        enabled = !isLocating,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("refresh_gps_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = stringResource(R.string.action_refresh_gps),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = "재측정",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
