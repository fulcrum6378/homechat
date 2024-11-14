package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index("id")],
    foreignKeys = [
        ForeignKey(
            entity = Message::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("msg"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Chat::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("chat"),
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
class Binary(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val title: String,
    val ext: String,
    val msg: Long,
    val chat: Short,
    val contact: Short,
) {
    fun internal() = "$id.$ext"
}
