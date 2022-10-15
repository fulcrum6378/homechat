package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

@Suppress("MemberVisibilityCanBePrivate")
class Device(srvInfo: NsdServiceInfo, mServiceName: String) : Radar.Item {
    val name: String = srvInfo.serviceName
    val host: InetAddress = srvInfo.host
    val port: Int = srvInfo.port
    val isMe: Boolean = srvInfo.serviceName == mServiceName
    val email: String? = srvInfo.attr(Contact.ATTR_EMAIL)
    val phone: String? = srvInfo.attr(Contact.ATTR_PHONE)

    @Transient
    var contact: Contact? = null

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
