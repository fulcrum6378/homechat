package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ir.mahdiparastesh.homechat.Sender

@Entity(
    primaryKeys = ["msg", "chat", "contact"],
    indices = [Index("chat"), Index("contact"), Index("msg")],
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
    val contact: Short
    var dateSent: Long?
    var dateSeen: Long?

    constructor(
        msg: Long, chat: Short, contact: Short, dateSent: Long? = null, dateSeen: Long? = null
    ) {
        this.chat = chat
        this.msg = msg
        this.contact = contact
        this.dateSent = dateSent
        this.dateSeen = dateSeen
    }
}
