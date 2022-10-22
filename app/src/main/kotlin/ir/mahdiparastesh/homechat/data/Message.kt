package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ir.mahdiparastesh.homechat.Sender

@Entity(
    primaryKeys = ["id", "chat"], // It is not possible with a composite primary key to add auto-increment.
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

    fun me() = from == Chat.ME
}
