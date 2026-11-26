package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.round

object Formatters {
    private val inLocale = Locale("en", "IN")

    private val dateFormat: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    }

    private val dateTimeFormat: ThreadLocal<SimpleDateFormat> = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    }

    fun round2(value: Double): Double = round(value * 100.0) / 100.0

    fun formatINR(amount: Double): String {
        val safe = round2(amount)
        val isNegative = safe < 0.0
        val absVal = kotlin.math.abs(safe)
        val totalPaise = kotlin.math.round(absVal * 100.0).toLong()
        val rupees = totalPaise / 100L
        val paise = (totalPaise % 100L).toInt()
        val rupeesFormatted = formatIndianInteger(rupees)
        val paiseStr = paise.toString().padStart(2, '0')
        return if (isNegative) "-₹$rupeesFormatted.$paiseStr" else "₹$rupeesFormatted.$paiseStr"
    }

    private fun formatIndianInteger(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRest,$lastThree"
    }

    fun formatDate(millis: Long): String = dateFormat.get()!!.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.get()!!.format(Date(millis))

    fun formatGrams(grams: Int): String {
        return if (grams >= 1000 && grams % 1000 == 0) {
            "${grams / 1000} kg"
        } else if (grams >= 1000) {
            String.format(inLocale, "%.2f kg", grams / 1000.0)
        } else {
            "$grams g"
        }
    }
}
