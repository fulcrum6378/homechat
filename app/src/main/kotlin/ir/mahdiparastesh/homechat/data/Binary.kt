package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Binary(
    @PrimaryKey(autoGenerate = true) val here: Long,
    val there: Long,
    val contact: Short,
    val title: String,
    val ext: String,
    val dateModified: Long,
    val dateReceived: Long,
) {
    fun file() = "$here.$ext"
}
