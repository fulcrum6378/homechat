package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import ir.mahdiparastesh.homechat.Sender

/**
 * It is not possible with a composite primary key to add auto-increment.
 */
@Entity(
    primaryKeys = ["id", "chat"], //
    indices = [Index("id"), Index("chat")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Message : Sender.Queuable {
    val id: Long
    val chat: Short
    val from: Short
    val type: Byte
    var data: String
    var repl: Long? // TODO make it changeable in UI
    var hide: Boolean
    val date: Long

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        id: Long, chat: Short, from: Short, type: Byte, data: String, repl: Long? = null,
        hide: Boolean = false, date: Long = Database.now()
    ) {
        this.id = id
        this.chat = chat
        this.from = from
        this.type = type
        this.data = data
        this.repl = repl
        this.hide = hide
        this.date = date
    }

    @Ignore
    @Transient
    var status: ArrayList<Seen>? = null

    fun me() = from == Chat.ME

    suspend fun matchSeen(dao: Database.DAO) {
        status = ArrayList(dao.seenForMessage(id, chat))
    }
}
