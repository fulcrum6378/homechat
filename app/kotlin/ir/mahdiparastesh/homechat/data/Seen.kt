package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Sender

@Suppress("PropertyName")
@Entity(
    tableName = "seen",
    primaryKeys = ["msg", "chat", "auth", "contact"],
    indices = [Index("msg"), Index("chat"), Index("auth"), Index("contact")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Seen : Sender.Queuable {
    val msg: Long
    val chat: Short
    val auth: Short
    val contact: Short
    var sent_at: Long?  // if contact is -1, dateSent is always null.
    var seen_at: Long?

    @Suppress("LocalVariableName")
    constructor(
        msg: Long, chat: Short, auth: Short,
        contact: Short, sent_at: Long? = null, seen_at: Long? = null
    ) {
        this.chat = chat
        this.msg = msg
        this.auth = auth
        this.contact = contact
        this.sent_at = sent_at
        this.seen_at = seen_at
    }

    override fun header(): Receiver.Header = Receiver.Header.SEEN

    override fun equals(other: Any?): Boolean {
        if (other !is Seen) return false
        return msg == other.msg && chat == other.chat && auth == other.auth && contact == other.contact
    }

    override fun hashCode(): Int {
        var result = msg.hashCode()
        result = 31 * result + chat
        result = 31 * result + auth
        result = 31 * result + contact
        return result
    }
}
