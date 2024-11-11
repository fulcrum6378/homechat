package ir.mahdiparastesh.homechat.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Time {
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US/*TODO?*/)
    val timeFormat = SimpleDateFormat("HH:mm"/*:ss*/, Locale.US)

    fun now() = Calendar.getInstance().timeInMillis

    fun Long.calendar(): Calendar = // from milliseconds
        Calendar.getInstance().apply { timeInMillis = this@calendar }
}
