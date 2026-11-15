package com.example.ui.dispatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeutralBorder
import com.example.ui.theme.NeutralTextPrimary
import com.example.ui.theme.NeutralTextSecondary
import com.example.ui.theme.PurpleSurfaceTint
import com.example.ui.theme.SecurityBlueLight
import com.example.ui.theme.SecurityBluePrimary
import com.example.ui.theme.TacticalGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ScheduleDateTimeFormatter {
    fun todayUtcMidnightMillis(): Long {
        val local = Calendar.getInstance()
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }.timeInMillis
    }

    fun tomorrowUtcMidnightMillis(): Long {
        val local = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }.timeInMillis
    }

    fun formatDate(utcMillis: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(utcMillis))
    }

    fun formatShortDate(utcMillis: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(utcMillis))
    }

    fun formatTime(hour24: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        val formatter = SimpleDateFormat("h:mm a", Locale.US)
        return formatter.format(cal.time)
    }

    fun formatFullStartDateTime(utcMillis: Long, hour24: Int, minute: Int): String {
        return "${formatDate(utcMillis)} at ${formatTime(hour24, minute)}"
    }

    fun formatEndTime(hour24: Int, minute: Int, durationHours: Long): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            add(Calendar.HOUR_OF_DAY, durationHours.toInt())
        }
        val formatter = SimpleDateFormat("h:mm a", Locale.US)
        return formatter.format(cal.time)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledDateTimeSection(
    selectedDateUtcMillis: Long,
    selectedHour: Int,
    selectedMinute: Int,
    durationHours: Long,
    onDateSelected: (Long) -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var chainTimeAfterDate by remember { mutableStateOf(false) }

    val todayUtcMillis = remember { ScheduleDateTimeFormatter.todayUtcMidnightMillis() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Schedule Start Date & Time",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = SecurityBluePrimary
            )

            TextButton(
                onClick = {
                    chainTimeAfterDate = true
                    showDatePicker = true
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("pick_datetime_flow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = SecurityBlueLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pick Date & Time",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecurityBlueLight
                )
            }
        }

        // Responsive Date and Time Picker triggers (side-by-side or stacked on narrow/large-font displays)
        val fontScale = LocalDensity.current.fontScale
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val stackButtons = maxWidth < 330.dp || fontScale >= 1.25f

            val dateButton: @Composable (Modifier) -> Unit = { btnMod ->
                Surface(
                    onClick = {
                        chainTimeAfterDate = false
                        showDatePicker = true
                    },
                    modifier = btnMod
                        .heightIn(min = 56.dp)
                        .testTag("pick_date_button"),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleSurfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Start Date",
                                tint = SecurityBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Start Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralTextSecondary
                            )
                            Text(
                                text = ScheduleDateTimeFormatter.formatShortDate(selectedDateUtcMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = NeutralTextPrimary
                            )
                        }
                    }
                }
            }

            val timeButton: @Composable (Modifier) -> Unit = { btnMod ->
                Surface(
                    onClick = { showTimePicker = true },
                    modifier = btnMod
                        .heightIn(min = 56.dp)
                        .testTag("pick_time_button"),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleSurfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Select Start Time",
                                tint = SecurityBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Start Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeutralTextSecondary
                            )
                            Text(
                                text = ScheduleDateTimeFormatter.formatTime(selectedHour, selectedMinute),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = NeutralTextPrimary
                            )
                        }
                    }
                }
            }

            if (stackButtons) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    dateButton(Modifier.fillMaxWidth())
                    timeButton(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    dateButton(Modifier.weight(1f))
                    timeButton(Modifier.weight(1f))
                }
            }
        }

        // Scheduled Window Preview Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PurpleSurfaceTint)
                .border(1.dp, SecurityBlueLight.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Starts ${ScheduleDateTimeFormatter.formatFullStartDateTime(selectedDateUtcMillis, selectedHour, selectedMinute)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = SecurityBluePrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ends ${ScheduleDateTimeFormatter.formatEndTime(selectedHour, selectedMinute, durationHours)}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TacticalGreen
            )
        }
    }

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= todayUtcMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                chainTimeAfterDate = false
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        showDatePicker = false
                        if (chainTimeAfterDate) {
                            chainTimeAfterDate = false
                            showTimePicker = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecurityBlueLight,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_date_button")
                ) {
                    Text(
                        text = if (chainTimeAfterDate) "Next: Set Time" else "Confirm Date",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        chainTimeAfterDate = false
                    }
                ) {
                    Text("Cancel", color = NeutralTextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = NeutralTextPrimary,
                    headlineContentColor = NeutralTextPrimary,
                    selectedDayContainerColor = SecurityBlueLight,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = SecurityBlueLight,
                    todayContentColor = SecurityBlueLight
                )
            )
        }
    }

    // Material 3 TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Select Start Time",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NeutralTextPrimary
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = Color(0xFFF1F5F9),
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = NeutralTextPrimary,
                            selectorColor = SecurityBlueLight,
                            periodSelectorBorderColor = NeutralBorder,
                            periodSelectorSelectedContainerColor = PurpleSurfaceTint,
                            periodSelectorSelectedContentColor = SecurityBlueLight,
                            periodSelectorUnselectedContainerColor = Color.White,
                            periodSelectorUnselectedContentColor = NeutralTextSecondary,
                            timeSelectorSelectedContainerColor = PurpleSurfaceTint,
                            timeSelectorSelectedContentColor = SecurityBlueLight,
                            timeSelectorUnselectedContainerColor = Color(0xFFF1F5F9),
                            timeSelectorUnselectedContentColor = NeutralTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecurityBlueLight,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_time_button")
                ) {
                    Text("Confirm Time", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = NeutralTextSecondary)
                }
            }
        )
    }
}
