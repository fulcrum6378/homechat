package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.util.Time

@Suppress("PropertyName")
@Entity(
    tableName = "binary",
    indices = [Index("id")],
)
class Binary(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val size: Long,
    val type: String?,
    val uri: String?,
    val created_at: Long,
) {
    constructor(size: Long, mime: String?, uri: String? = null) :
            this(0L, size, mime, uri, Time.now())
}
