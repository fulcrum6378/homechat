package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

@Suppress("MemberVisibilityCanBePrivate")
class Device(srvInfo: NsdServiceInfo) : Radar.Item, Radar.Named {
    val name: String = srvInfo.serviceName
    val host: InetAddress = srvInfo.host
    val port: Int = srvInfo.port
    val unique: String? = srvInfo.attr(Contact.ATTR_UNIQUE)

    @Transient
    var contact: Contact? = null

    fun NsdServiceInfo.attr(key: String): String? = attributes[key]?.let { String(it) }

    suspend fun matchContact(m: Model, dao: Database.DAO) {
        val matches = m.contacts?.filter { it.equals(this) }
        if (matches == null) {
            contact = null
            return; }
        contact = if (matches.size == 1) matches[0] else null

        // Update IP and port
        contact?.apply {
            if (ip != host.hostAddress || this@apply.port != this@Device.port) {
                ip = host.hostAddress
                port = this@Device.port
                dao.updateContact(this)
            }
        }

        // Remove this IP address from any other Contact
        m.contacts?.forEach {
            if (it.id == contact?.id) return@forEach
            if (it.ip == contact?.ip) {
                it.ip = null
                it.port = null
                dao.updateContact(it)
            }
        }
    }

    override fun name(): String = unique ?: name

    override fun toString(): String = "${host.hostAddress}:$port"

    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> host.hostAddress == other.host.hostAddress
        is Contact -> if (unique != null) unique == other.unique else name == other.device
        else -> false
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + (unique?.hashCode() ?: 0)
        return result
    }

    companion object {
        fun String.makeAddressPair(): Pair<String, Int> =
            split(":").let { Pair(it[0], it[1].toInt()) }
    }
}
