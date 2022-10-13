package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["id", "msg"],
    indices = [Index("id"), Index("msg")],
    foreignKeys = [ForeignKey(
        entity = Message::class,
        parentColumns = arrayOf("id", "chat"),
        childColumns = arrayOf("msg", "chat"),
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Contact::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("contact"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Seen(
    val chat: Short,
    val msg: Long,
    val contact: Short,
    val date: Long,
) {
    var id = 0L
}
