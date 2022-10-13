package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Contact(
    @PrimaryKey var id: Short, // Room does not support unsigned numbers
    var name: String,
    var lastIp: String,
    var dateCreated: Long,
    var email: String? = null,
    var phone: String? = null,
    var lastOnline: Long? = null,
) {
    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> name == other.name && lastIp == other.host.hostAddress &&
                email == other.email && phone == other.phone
        is Contact -> toString() == other.toString()
        else -> false
    }

    override fun hashCode(): Int = id.hashCode()

    companion object {
        const val ATTR_EMAIL = "email"
        const val ATTR_PHONE = "phone"
    }
}
