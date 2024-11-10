package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import ir.mahdiparastesh.homechat.Sender

@Entity(
    primaryKeys = ["id", "chat", "auth"],
    indices = [Index("chat"), Index("time")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Message : Sender.Queuable {
    val id: Long  // It is not possible with a composite primary key to add auto-increment.
    val chat: Short
    val auth: Short
    val type: Byte
    var data: String
    var repl: Long?
    var hide: Boolean
    val time: Long

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        id: Long, chat: Short, auth: Short, type: Byte, data: String, repl: Long? = null,
        hide: Boolean = false, time: Long = Database.now()
    ) {
        this.id = id
        this.chat = chat
        this.auth = auth
        this.type = type
        this.data = data
        this.repl = repl
        this.hide = hide
        this.time = time
    }

    @Ignore
    @Transient
    var status: ArrayList<Seen>? = null

    fun me() = auth == Chat.ME

    suspend fun matchSeen(dao: Database.DAO) {
        status = ArrayList(dao.seenForMessage(id, chat))
    }

    fun shorten(max: Int = 30): String =
        if (data.length > max) data.substring(0, max) else data
}
