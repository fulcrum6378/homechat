package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

@Suppress("MemberVisibilityCanBePrivate")
class Device(srvInfo: NsdServiceInfo, mServiceName: String) {
    val host: InetAddress
    val port: Int
    val name: String
    val isMe: Boolean
    var lat: Double? = null
    var lng: Double? = null

    init {
        host = srvInfo.host
        port = srvInfo.port
        name = srvInfo.serviceName
        isMe = srvInfo.serviceName == mServiceName
        srvInfo.attributes["location"]?.let { String(it) }?.split(",")?.also {
            lat = it[0].toDouble()
            lng = it[1].toDouble()
        }
    }

    override fun toString(): String = "${host.hostAddress}:$port"

    override fun equals(other: Any?): Boolean {
        if (other !is Device) return false
        return toString() == other.toString()
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        return result
    }
}
