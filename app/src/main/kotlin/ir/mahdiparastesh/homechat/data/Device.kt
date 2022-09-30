package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import java.net.InetAddress

class Device(
    private val host: InetAddress, private val port: Int, val service: String, private val isMe: Boolean
) {
    constructor(srvInfo: NsdServiceInfo, mServiceName: String) : this(
        srvInfo.host, srvInfo.port, srvInfo.serviceName,
        srvInfo.serviceName == mServiceName
    )

    override fun toString(): String = "${host.hostAddress}:$port"

    override fun equals(other: Any?): Boolean {
        if (other !is Device) return false
        return toString() == other.toString()
    }

    override fun hashCode(): Int {
        var result = host.hashCode()
        result = 31 * result + port
        result = 31 * result + service.hashCode()
        result = 31 * result + isMe.hashCode()
        return result
    }
}
