package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Message(
    val chat: Long,
    val type: Byte,
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L

    enum class Type(value: Byte) {
        TEXT(0),
    }
}
