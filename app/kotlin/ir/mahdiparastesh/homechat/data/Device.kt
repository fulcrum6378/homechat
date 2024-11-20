package ir.mahdiparastesh.homechat.data

import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.base.Persistent
import ir.mahdiparastesh.homechat.page.PageSet
import ir.mahdiparastesh.homechat.toByteArray
import ir.mahdiparastesh.homechat.toNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlin.collections.joinToString

class Device(srvInfo: NsdServiceInfo) : Radar.Item, Radar.Named {
    /** temporarily unique */
    val name: String = srvInfo.serviceName
    val host: InetAddress
    val port: Int = srvInfo.port
    val unique: String? = srvInfo.attr(PageSet.PRF_UNIQUE)

    @Transient
    var contact: Contact? = null

    init {
        @Suppress("DEPRECATION")
        host =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7
            ) srvInfo.hostAddresses[0]
            else srvInfo.host
    }


    fun NsdServiceInfo.attr(key: String): String? = attributes[key]?.let { String(it) }

    fun address(): String = "${host.hostAddress}:$port"

    fun addressPair(): Pair<String, Int> =
        address().split(":").let { Pair(it[0], it[1].toInt()) }

    suspend fun pair(c: Persistent, error: (msg: Int) -> Unit, success: () -> Unit) {
        val address = addressPair()
        Transmitter(address, Receiver.Header.PAIR, {
            val contacts = c.dao.contactIds().joinToString(",").encodeToByteArray()
            val chats = c.dao.chatIds().joinToString(",").encodeToByteArray()
            contacts.size.toShort().toByteArray().plus(contacts)
                .plus(chats.size.toShort().toByteArray()).plus(chats)
        }, {
            withContext(Dispatchers.Main) { error(R.string.pairFailedToConnect) }
        }) { res ->
            if (ByteBuffer.wrap(res).int == -1) {
                withContext(Dispatchers.Main) { error(R.string.pairFailedToPair) }
                return@Transmitter; }

            val lb = res.toList()
            val contactId = lb.subList(0, 2).toNumber<Short>()
            val chatId = lb.subList(2, 4).toNumber<Short>()
            Contact(contactId, this).apply {
                c.dao.addContact(this)
                c.m.contacts?.add(this)
            }
            Chat(chatId, contactId.toString()).apply {
                c.dao.addChat(this)
                c.m.chats?.add(this)
            }
            c.m.radar.update(c.dao)
            success()
        }
    }

    suspend fun matchContact(m: Model, dao: Database.DAO) {
        val matches = m.contacts?.filter { it.equals(this) }
        if (matches == null) {
            contact = null
            return; }
        contact = if (matches.size == 1) matches[0] else null

        // update IP and port
        contact?.apply {
            if (ip != host.hostAddress || this@apply.port != this@Device.port) {
                ip = host.hostAddress
                port = this@Device.port
                dao.updateContact(this)
            }
        }

        // remove this IP address from any other Contact
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

    override fun equals(other: Any?): Boolean = when (other) {
        is Device -> if (unique != null) unique == other.unique else name == other.name
        is Contact -> if (unique != null) unique == other.unique else name == other.device
        else -> false
    }

    override fun hashCode(): Int = name.hashCode()
}
