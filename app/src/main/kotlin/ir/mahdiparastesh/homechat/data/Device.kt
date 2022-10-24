package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

@Suppress("MemberVisibilityCanBePrivate")
class Device(srvInfo: NsdServiceInfo) : Radar.Item {
    val name: String = srvInfo.serviceName
    val host: InetAddress = srvInfo.host
    val port: Int = srvInfo.port
    val email: String? = srvInfo.attr(Contact.ATTR_EMAIL)
    val phone: String? = srvInfo.attr(Contact.ATTR_PHONE)

    @Transient
    var contact: Contact? = null

    fun NsdServiceInfo.attr(key: String): String? = attributes[key]?.let { String(it) }

    suspend fun matchContact(m: Model, dao: Database.DAO) {
        val matches = m.contacts?.filter { it.equals(this) }
        if (matches == null) {
            contact = null
            return; }
        contact = if (matches.size == 1) matches[0] else null
        contact?.apply {
            if (lastIp != host.hostAddress) {
                lastIp = host.hostAddress
                dao.updateContact(this)
            }
        }
        m.contacts?.forEach {
            if (it.id == contact?.id) return@forEach
            if (it.lastIp == contact?.lastIp) {
                it.lastIp = null
                dao.updateContact(it)
            }
        }
    }

    override fun toString(): String = "${host.hostAddress}:$port"

    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> host.hostAddress == other.host.hostAddress
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
