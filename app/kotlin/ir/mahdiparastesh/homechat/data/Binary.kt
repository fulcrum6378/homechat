package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "binary",
    indices = [Index("id")],
)
class Binary(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val ext: String,
    val name: String,
) {
    fun internal() = "$id.$ext"
}
