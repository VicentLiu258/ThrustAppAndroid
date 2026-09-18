package com.thrustwealth.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object FormatUtils {
    fun currencySymbol(code: String): String = when (code) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY" -> "¥"
        else -> "$"
    }

    fun formatMoney(value: Double, currency: String = "USD", digits: Int = 2): String {
        val symbol = currencySymbol(currency)
        val body = "%,.${digits}f".format(Locale.US, abs(value))
        return if (value < 0) "-$symbol$body" else "$symbol$body"
    }

    fun formatSignedMoney(value: Double, currency: String = "USD"): String {
        val symbol = currencySymbol(currency)
        val body = "%,.2f".format(Locale.US, abs(value))
        return when {
            value > 0 -> "+$symbol$body"
            value < 0 -> "-$symbol$body"
            else -> "$symbol$body"
        }
    }

    fun formatLots(lots: Double): String = "%.2f".format(Locale.US, lots)

    fun formatPercent(value: Double, digits: Int = 1): String =
        "%.${digits}f%%".format(Locale.US, value)

    fun formatPrice(value: Double, digits: Int): String =
        "%,.${digits}f".format(Locale.US, value)

    fun formatTime(millis: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

    fun formatDate(millis: Long): String =
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(millis))

    fun formatDateGroup(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance()
        if (isSameDay(cal, today)) return "TODAY"
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (isSameDay(cal, yesterday)) return "YESTERDAY"
        return formatDate(millis).uppercase(Locale.getDefault())
    }

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
