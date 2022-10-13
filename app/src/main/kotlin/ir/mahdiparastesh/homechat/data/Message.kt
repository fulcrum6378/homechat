package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["id", "chat"],
    indices = [Index("id"), Index("chat")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Message(
    val data: String,
    val chat: Short,
    val type: Byte,
    val date: Long,
    val hide: Boolean = false,
) {
    var id = 0L

    enum class Type
}
