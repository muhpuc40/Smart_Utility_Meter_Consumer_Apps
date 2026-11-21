package com.smartmeter.consumer.ui.util

import androidx.compose.ui.graphics.Color
import com.smartmeter.consumer.ui.theme.DangerColor
import com.smartmeter.consumer.ui.theme.SuccessColor
import com.smartmeter.consumer.ui.theme.WarningColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTimeFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.US)
private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
private val dayFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.US)

// ------------------------------------------------------------
// Numbers
// ------------------------------------------------------------

fun money(value: Double): String = String.format(Locale.US, "৳%,.2f", value)

fun number(value: Double?, decimals: Int = 2): String =
    if (value == null) "--" else String.format(Locale.US, "%,.${decimals}f", value)

// ------------------------------------------------------------
// Dates (server sends ISO-8601 timestamps)
// ------------------------------------------------------------

fun parseInstant(iso: String?): Instant? =
    iso?.let { runCatching { OffsetDateTime.parse(it).toInstant() }.getOrNull() }

fun formatDateTime(iso: String?): String =
    parseInstant(iso)?.atZone(ZoneId.systemDefault())?.format(dateTimeFormat) ?: "--"

fun formatDate(iso: String?): String =
    parseInstant(iso)?.atZone(ZoneId.systemDefault())?.format(dateFormat) ?: "--"

// "2026-11-19" -> "19 Nov"
fun formatDay(day: String): String =
    runCatching { LocalDate.parse(day).format(dayFormat) }.getOrDefault(day)

fun timeAgo(iso: String?): String {
    val instant = parseInstant(iso) ?: return "never"
    val seconds = Duration.between(instant, Instant.now()).seconds.coerceAtLeast(0)

    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60} min ago"
        seconds < 86400 -> "${seconds / 3600} h ago"
        else -> "${seconds / 86400} d ago"
    }
}

// ------------------------------------------------------------
// Meter state (from the backend)
// ------------------------------------------------------------

fun stateLabel(state: String): String = when (state) {
    "active" -> "Supply ON"
    "no_balance" -> "Balance zero · Supply OFF"
    "meter_disabled" -> "Meter disabled by office"
    "consumer_suspended" -> "Account suspended"
    else -> "Supply OFF"
}

fun relayColor(relay: String): Color = if (relay == "on") SuccessColor else DangerColor

// ------------------------------------------------------------
// Recharges
// ------------------------------------------------------------

// Methods a consumer can use (cash is admin only)
val RECHARGE_METHODS = listOf("bkash", "nagad", "rocket", "upay", "card", "bank", "other")

fun methodLabel(method: String): String = when (method) {
    "bkash" -> "bKash"
    "nagad" -> "Nagad"
    "rocket" -> "Rocket"
    "upay" -> "Upay"
    "card" -> "Card"
    "bank" -> "Bank"
    "cash" -> "Cash"
    else -> "Other"
}

fun rechargeStatusColor(status: String): Color = when (status) {
    "approved" -> SuccessColor
    "rejected" -> DangerColor
    else -> WarningColor
}

fun capitalized(text: String): String = text.replaceFirstChar { it.uppercase() }
