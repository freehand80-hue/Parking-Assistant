package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.ParkingReminderScheduler
import com.example.ui.ParkingTimeFormatter
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun ParkingReminderCard(
    reminderEnabled: Boolean,
    hasNotificationPermission: Boolean,
    savedAtMillis: Long?,
    nowMillis: Long,
    onToggleReminder: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val triggerAtMillis = savedAtMillis?.let { it + ParkingReminderScheduler.REMINDER_DELAY_MILLIS }
    val isTriggered = triggerAtMillis != null && nowMillis >= triggerAtMillis
    val isActiveScheduled = reminderEnabled && hasNotificationPermission && triggerAtMillis != null && !isTriggered

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.5.dp,
            color = when {
                reminderEnabled && !hasNotificationPermission -> MaterialTheme.colorScheme.error
                isActiveScheduled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (reminderEnabled) {
                            Icons.Filled.NotificationsActive
                        } else {
                            Icons.Filled.NotificationsOff
                        },
                        contentDescription = null,
                        tint = if (reminderEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_card_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isActiveScheduled && triggerAtMillis != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = ParkingTimeFormatter.formatRemainingDuration(
                                            triggerAtMillis,
                                            nowMillis
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("reminder_remaining_time_text")
                                    )
                                }
                            }
                        }
                        Text(
                            text = when {
                                !reminderEnabled -> stringResource(R.string.reminder_status_off)
                                !hasNotificationPermission -> stringResource(R.string.reminder_status_permission_needed)
                                triggerAtMillis == null -> stringResource(R.string.reminder_status_auto_on_save)
                                isTriggered -> stringResource(R.string.reminder_status_triggered)
                                else -> stringResource(
                                    R.string.reminder_status_scheduled_format,
                                    ParkingTimeFormatter.formatClockTime(triggerAtMillis)
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when {
                                reminderEnabled && !hasNotificationPermission -> MaterialTheme.colorScheme.error
                                isTriggered -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isActiveScheduled || isTriggered) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.size(6.dp))

                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onToggleReminder,
                    modifier = Modifier.testTag("parking_reminder_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            if (reminderEnabled && !hasNotificationPermission) {
                Button(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_notification_permission_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.reminder_action_grant_permission),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
