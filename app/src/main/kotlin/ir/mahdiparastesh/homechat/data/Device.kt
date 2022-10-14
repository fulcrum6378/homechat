package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

@Suppress("MemberVisibilityCanBePrivate")
class Device(srvInfo: NsdServiceInfo, mServiceName: String) {
    val host: InetAddress
    val port: Int
    val name: String
    val isMe: Boolean
    val email: String?
    val phone: String?

    @Transient
    var contact: Contact? = null

    init {
        host = srvInfo.host
        port = srvInfo.port
        name = srvInfo.serviceName
        isMe = srvInfo.serviceName == mServiceName
        email = srvInfo.attr(Contact.ATTR_EMAIL)
        phone = srvInfo.attr(Contact.ATTR_PHONE)
    }

    fun NsdServiceInfo.attr(key: String): String? = attributes[key]?.let { String(it) }

    fun matchContact(list: List<Contact>?) {
        if (list == null) return
        val matches = list.filter { it.equals(this) }
        contact = if (matches.size == 1) matches[0] else null
    }

    override fun toString(): String = "${host.hostAddress}:$port"

    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> toString() == other.toString()
        is Contact -> name == other.name && (email == other.email || phone == other.phone)
        else -> false
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        return result
    }

    companion object {
        fun String.makeAddressPair(): Pair<String, Int> =
            split(":").let { Pair(it[0], it[1].toInt()) }
    }
}
