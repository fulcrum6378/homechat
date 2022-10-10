package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Message::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("msg"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Seen(
    val msg: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
}
