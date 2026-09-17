package com.programovil.aura.shared.presentation

import androidx.compose.runtime.Composable
import aura_app.composeapp.generated.resources.*
import kotlinx.datetime.*
import org.jetbrains.compose.resources.*

fun localDate(millis: Long) = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
fun todayDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

@Composable fun auraDate(date: LocalDate): String {
    val months = stringArrayResource(Res.array.rd_months)
    return stringResource(Res.string.rd_date_label, date.dayOfMonth, months[date.monthNumber - 1], date.year)
}
@Composable fun auraMonth(date: LocalDate) = "${stringArrayResource(Res.array.rd_months)[date.monthNumber - 1]} ${date.year}"
@Composable fun auraDay(date: LocalDate) = stringArrayResource(Res.array.rd_weekdays)[date.dayOfWeek.isoDayNumber - 1]

@Composable
fun auraClockTime(hour: Int, minute: Int, is24Hour: Boolean): String {
    val minutes = minute.toString().padStart(2, '0')
    if (is24Hour) return "${hour.toString().padStart(2, '0')}:$minutes"
    val displayHour = (hour % 12).let { if (it == 0) 12 else it }
    return "$displayHour:$minutes " + stringResource(if (hour < 12) Res.string.rd_am else Res.string.rd_pm)
}
