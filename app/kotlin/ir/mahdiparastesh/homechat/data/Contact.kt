package ir.mahdiparastesh.homechat.data

import androidx.core.app.Person
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.util.Time

@Entity(indices = [Index("id")])
class Contact(
    @PrimaryKey var id: Short, // Room does not support unsigned numbers!
    var device: String,
    var unique: String?,
    var ip: String?,
    var port: Int?,
    var lastOnline: Long? = null,
    val createdAt: Long = Time.now(),
) : Radar.Named {

    // obtaining MAC address is almost impossible in the newer APIs!
    constructor(id: Short, dev: Device) : this(
        id, dev.name, dev.unique, dev.host.hostAddress!!, dev.port, Time.now()
    )

    fun person(isImportant: Boolean): Person = Person.Builder()
        .setName(unique ?: device)
        .setKey(id.toString())
        .setImportant(isImportant)
        .build()

    override fun name(): String = unique ?: device
    override fun toString(): String = id.toString()
    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = when (other) {
        is Contact -> toString() == other.toString()
        is Device -> if (unique != null) unique == other.unique else device == other.name
        else -> false
    }
}
