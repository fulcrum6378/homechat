package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ir.mahdiparastesh.homechat.Sender

/**
 * Only created for the Contacts, NOT ME!!
 * YOURS is created in the Contacts' database!
 * don't make a foreign key for "msg"; 'cus it got no unique index!
 */
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
class Seen : Sender.Queuable {
    val msg: Long
    val chat: Short
    val contact: Short
    var dateSent: Long?
    var dateSeen: Long?

    @Suppress("ConvertSecondaryConstructorToPrimary")
    constructor(
        msg: Long, chat: Short, contact: Short, dateSent: Long? = null, dateSeen: Long? = null
    ) {
        this.chat = chat
        this.msg = msg
        this.contact = contact
        this.dateSent = dateSent
        this.dateSeen = dateSeen
    }
}
