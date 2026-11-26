package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.InstalledMapApps
import com.example.data.MapAppOption

@Composable
fun FindMyCarNavigationCard(
    hasCoordinates: Boolean,
    selectedMapApp: MapAppOption,
    installedMaps: InstalledMapApps,
    onSelectMapApp: (MapAppOption) -> Unit,
    onNavigateClick: () -> Unit,
    fillHeight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (hasCoordinates) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = (if (fillHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val buttonModifier = if (fillHeight) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 56.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            }

            Button(
                onClick = onNavigateClick,
                enabled = hasCoordinates,
                modifier = buttonModifier.testTag("find_my_car_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 1.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.action_find_my_car),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.action_find_my_car_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasCoordinates) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MapOptionChip(
                    title = stringResource(R.string.map_option_auto),
                    installed = false,
                    selected = selectedMapApp == MapAppOption.AUTO,
                    onClick = { onSelectMapApp(MapAppOption.AUTO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_option_auto")
                )
                MapOptionChip(
                    title = stringResource(R.string.map_option_kakao),
                    installed = installedMaps.hasKakaoMap,
                    selected = selectedMapApp == MapAppOption.KAKAO,
                    onClick = { onSelectMapApp(MapAppOption.KAKAO) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_option_kakao")
                )
                MapOptionChip(
                    title = stringResource(R.string.map_option_naver),
                    installed = installedMaps.hasNaverMap,
                    selected = selectedMapApp == MapAppOption.NAVER,
                    onClick = { onSelectMapApp(MapAppOption.NAVER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_option_naver")
                )
                MapOptionChip(
                    title = stringResource(R.string.map_option_google),
                    installed = installedMaps.hasGoogleMaps,
                    selected = selectedMapApp == MapAppOption.GOOGLE,
                    onClick = { onSelectMapApp(MapAppOption.GOOGLE) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("map_option_google")
                )
            }
        }
    }
}

@Composable
private fun MapOptionChip(
    title: String,
    installed: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (installed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = stringResource(R.string.map_status_installed),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }
        }
    }
}
