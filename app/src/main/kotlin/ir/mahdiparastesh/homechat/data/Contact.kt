package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.more.Persistent

@Entity
class Contact(
    @PrimaryKey var id: Short, // Room does not support unsigned numbers!
    var name: String,
    var ip: String?,
    var port: Int,
    var email: String? = null,
    var phone: String? = null,
    var lastOnline: Long? = null,
    val dateCreated: Long = Database.now(),
    var muted: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> name == other.name && (email == other.email || phone == other.phone)
        is Contact -> toString() == other.toString()
        else -> false
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        // obtaining MAC address is almost impossible in the newer APIs!
        const val ATTR_EMAIL = "email"
        const val ATTR_PHONE = "phone"

        suspend fun postPairing(c: Persistent, chosenId: Short, dev: Device): Contact = Contact(
            chosenId, dev.name, dev.host.hostAddress!!, dev.port,
            dev.email, dev.phone, Database.now()
        ).also {
            c.dao.addContact(it)
            c.m.contacts?.add(it)
        }
    }
}
