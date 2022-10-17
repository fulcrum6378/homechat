package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    primaryKeys = ["msg", "chat", "contact"],
    indices = [Index("msg"), Index("chat"), Index("contact")],
    foreignKeys = [ForeignKey(
        entity = Chat::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("chat"),
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Contact::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("contact"),
        onDelete = ForeignKey.CASCADE
    )]
)
class Seen {
    val msg: Long // don't make a foreign key for this; 'cus it got no unique index!
    val chat: Short
    val contact: Short
    val date: Long

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(chat: Short, msg: Long, contact: Short, date: Long) {
        this.chat = chat
        this.msg = msg
        this.contact = contact
        this.date = date
    }
}
