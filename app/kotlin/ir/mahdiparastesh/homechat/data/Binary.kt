package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Sender
import ir.mahdiparastesh.homechat.util.Time

@Suppress("PropertyName")
@Entity(
    tableName = "binary",
    indices = [Index("id")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Binary(
    @PrimaryKey(autoGenerate = true) var id: Long,
    val alias: Long?,
    val size: Long,
    val type: String?,
    val uri: String?,
    val pos_in_msg: Byte,
    val msg: Long,
    val chat: Short,
    val auth: Short,
    val source_id: Long?,
    val created_at: Long,
) : Sender.Queuable {
    // source device
    constructor(
        size: Long, type: String?, uri: String?,
        posInMsg: Byte, msg: Long, chat: Short, auth: Short,
        alias: Long? = null,
    ) : this(0L, alias, size, type, uri, posInMsg, msg, chat, auth, null, Time.now())

    // target device
    constructor(
        size: Long, type: String?, sourceId: Long, createdAt: Long,
        posInMsg: Byte, msg: Long, chat: Short, auth: Short,
    ) : this(0L, null, size, type, null, posInMsg, msg, chat, auth, sourceId, createdAt)


    override fun header(): Receiver.Header = Receiver.Header.BINARY
}
