package ir.mahdiparastesh.homechat.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import ir.mahdiparastesh.homechat.R

@SuppressLint("NewApi")
class Notify {
    enum class Channel(
        val id: String, // unique within this app, recommended maximum 40 characters
        @StringRes val rName: Int,
        @StringRes val rDesc: Int,
        private val importance: Int = NotificationManager.IMPORTANCE_LOW,
        private val groupId: String? = null,
    ) {
        NEW_MESSAGE(
            "new_msg", R.string.ntfNewMessage, R.string.ntfNewMessageDesc,
            NotificationManager.IMPORTANCE_HIGH, ChannelGroup.CHAT.id
        );

        @RequiresApi(Build.VERSION_CODES.O)
        fun create(c: Context) = NotificationChannel(id, c.resources.getString(rName), importance)
            .apply {
                description = c.resources.getString(rDesc)
                group = groupId
            }
    }

    enum class ChannelGroup(
        val id: String,
        @StringRes val rName: Int,
        @StringRes val rDesc: Int,
    ) {
        CHAT("chat", R.string.ntfGrpChat, R.string.ntfGrpChatDesc);

        @RequiresApi(Build.VERSION_CODES.O)
        fun create(c: Context) = NotificationChannelGroup(id, c.resources.getString(rName))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    description = c.resources.getString(rDesc)
            }
    }

    companion object {
        fun mutability(bb: Boolean = false): Int = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (bb) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
            else ->
                if (bb) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_IMMUTABLE
        }
    }
}