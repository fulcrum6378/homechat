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
    ), ForeignKey(
        entity = Contact::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("contact"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Seen(
    val msg: Long,
    val contact: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
}
