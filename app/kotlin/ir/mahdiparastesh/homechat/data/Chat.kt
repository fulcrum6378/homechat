package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.more.Persistent

@Entity(indices = [Index("id")])
class Chat(
    @PrimaryKey var id: Short = 0,
    var contactIds: String, // separated by CONTACT_SEP
    var name: String? = null,
    val dateInit: Long = Database.now(),
    var muted: Boolean = false,
) : Radar.Item {

    @Ignore
    @Transient
    var contacts: List<Contact>? = null

    fun isDirect() = (contacts?.size ?: contactIds.split(CONTACT_SEP).size) == 1

    fun matchContacts(allContacts: List<Contact>) {
        contacts = contactIds.split(CONTACT_SEP)
            .map { id -> allContacts.find { it.id == id.toShort() }!! }
    }

    companion object {
        const val CONTACT_SEP = ","
        const val ME = (-1).toShort()

        suspend fun postInitiation(c: Persistent, chosenId: Short, contactIds: String) {
            Chat(chosenId, contactIds).also {
                c.dao.addChat(it)
                c.m.chats?.add(it)
            }
            c.m.radar.update(c.dao)
        }
    }
}
