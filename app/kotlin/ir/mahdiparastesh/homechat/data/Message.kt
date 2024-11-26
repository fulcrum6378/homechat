package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Sender
import ir.mahdiparastesh.homechat.util.Time

@Entity(
    tableName = "message",
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
        hide: Boolean = false, time: Long = Time.now()
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


    override fun header(): Receiver.Header = Receiver.Header.MESSAGE

    fun me() = auth == Chat.ME

    suspend fun matchSeen(dao: Database.DAO) {
        status = ArrayList(dao.seenForMessage(id, chat))
    }

    fun shorten(max: Int = 30): String =
        if (data.length > max) data.substring(0, max) else data

    override fun equals(other: Any?): Boolean {
        if (other !is Message) return false
        return id == other.id && chat == other.chat && auth == other.auth
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + chat
        result = 31 * result + auth
        return result
    }

    companion object {
        const val BINARY_SEP = ","
    }

    enum class Type(val value: Byte) {
        TEXT(0x00),
        FILE(0x01),
    }
}
