package ir.mahdiparastesh.homechat.util

import android.content.Context
import androidx.annotation.StringRes
import ir.mahdiparastesh.homechat.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

object Time {
    const val PATTERN_DATE = "MMMM d"
    const val PATTERN_DATE_FULL = "yyyy.MM.dd"
    const val PATTERN_TIME = "HH:mm"/*:ss*/
    const val SECOND = 1000L
    const val MINUTE = 60000L
    const val HOUR = 3600000L
    const val DAY = 86400000L
    const val WEEK = 604800000L
    val thisYear = cal()[Calendar.YEAR]


    fun cal() = Calendar.getInstance()

    fun now() = cal().timeInMillis

    fun Long.calendar(): Calendar = // from milliseconds
        cal().apply { timeInMillis = this@calendar }

    fun formatDate(cal: Calendar): String =
        if (cal[Calendar.YEAR] == thisYear)
            SimpleDateFormat(PATTERN_DATE, Locale.getDefault()).format(cal.timeInMillis)
        else SimpleDateFormat(PATTERN_DATE_FULL, Locale.getDefault()).format(cal.timeInMillis)

    fun formatTime(time: Long): String =
        SimpleDateFormat(PATTERN_TIME, Locale.getDefault()).format(time)

    fun distance(
        c: Context, a: Calendar, b: Calendar = cal(), @StringRes append: Int? = null
    ): String {
        val dif = abs(a.time.time - b.time.time)
        val append = if (append != null) c.getString(append) else ""
        return when {
            dif < (SECOND * 15) -> c.getString(R.string.justNow)
            dif < MINUTE -> c.getString(R.string.seconds, (dif / SECOND).toInt()) + append
            dif < HOUR -> {
                val unit = (dif / MINUTE).toInt()
                c.resources.getQuantityString(R.plurals.minute, unit, unit) + append
            }
            dif < DAY -> {
                val unit = (dif / HOUR).toInt()
                c.resources.getQuantityString(R.plurals.hour, unit, unit) + append
            }
            dif < WEEK -> {
                val unit = (dif / DAY).toInt()
                c.resources.getQuantityString(R.plurals.day, unit, unit) + append
            }
            else -> {
                val unit = (dif / WEEK).toInt()
                c.resources.getQuantityString(R.plurals.week, unit, unit) + append
            }
        }
    }
}
