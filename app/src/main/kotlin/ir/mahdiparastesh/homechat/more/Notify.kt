package ir.mahdiparastesh.homechat.more

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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

        fun create(c: Context) = NotificationChannelGroup(id, c.resources.getString(rName))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    description = c.resources.getString(rDesc)
            }
    }
}