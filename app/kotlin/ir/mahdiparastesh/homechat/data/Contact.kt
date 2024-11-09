package ir.mahdiparastesh.homechat.data

import androidx.core.app.Person
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.more.Persistent

@Entity(indices = [Index("id")])
class Contact(
    @PrimaryKey var id: Short, // Room does not support unsigned numbers!
    var device: String,
    var unique: String?,
    var ip: String?,
    var port: Int?,
    var lastOnline: Long? = null,
    val dateCreated: Long = Database.now(),
    var isFav: Boolean = false,
) : Radar.Named {
    override fun toString(): String = id.toString()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is Contact -> toString() == other.toString()
        is Device -> if (unique != null) unique == other.unique else device == other.name
        else -> false
    }

    override fun name(): String = unique ?: device

    fun person(): Person = Person.Builder()
        .setName(unique ?: device)
        .setKey(id.toString())
        .setImportant(isFav)
        .build()

    companion object {
        // obtaining MAC address is almost impossible in the newer APIs!
        suspend fun postPairing(c: Persistent, chosenId: Short, dev: Device): Contact = Contact(
            chosenId, dev.name, dev.unique, dev.host.hostAddress!!, dev.port, Database.now()
        ).also {
            c.dao.addContact(it)
            c.m.contacts?.add(it)
        }
    }
}
