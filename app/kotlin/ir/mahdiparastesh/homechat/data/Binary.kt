package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Suppress("PropertyName")
@Entity(
    tableName = "binary",
    indices = [Index("id")],
)
class Binary(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val ext: String,
    val name: String,
    val size: Long,
    val date_modified: Long,
    val path: String?,
    val created_at: Long,
)
