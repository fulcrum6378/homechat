package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Binary(
    @PrimaryKey(autoGenerate = true) val here: Long,
    val there: Long, // TODO what about a group chat
    val contact: Short,
    val title: String,
    val ext: String,
) {
    fun file() = "$here.$ext"
}
