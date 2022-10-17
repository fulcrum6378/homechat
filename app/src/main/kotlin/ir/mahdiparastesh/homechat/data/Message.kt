package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

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
class Message {
    val id: Long
    val chat: Short
    val type: Byte
    val data: String
    var seen: Boolean // only for second person, first person always "false"
    val hide: Boolean
    val date: Long

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        id: Long, chat: Short, type: Byte, data: String,
        seen: Boolean = false, hide: Boolean = false, date: Long = Database.now()
    ) {
        this.id = id
        this.chat = chat
        this.type = type
        this.data = data
        this.seen = seen
        this.hide = hide
        this.date = date
    }

    enum class Type
}
