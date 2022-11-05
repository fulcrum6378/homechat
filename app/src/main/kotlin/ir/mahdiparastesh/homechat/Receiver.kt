package ir.mahdiparastesh.homechat

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.net.IpSecManager.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.mahdiparastesh.homechat.data.*
import ir.mahdiparastesh.homechat.more.Notify
import ir.mahdiparastesh.homechat.more.WiseService
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.page.PageSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.min

@Suppress("BlockingMethodInNonBlockingContext")
class Receiver : WiseService() {
    private lateinit var server: ServerSocket
    private val ipToContactId = hashMapOf<String, Short>()
    private lateinit var nm: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        m.aliveReceiver = true
        nm = NotificationManagerCompat.from(c)
        nm.createNotificationChannelGroup(Notify.ChannelGroup.CHAT.create(c))
        nm.createNotificationChannel(Notify.Channel.NEW_MESSAGE.create(c))
    }

    /** A new instance of Receiver is created on the sticky resurrection.
     * START_STICKY_COMPATIBILITY does not guarantee onStartCommand! */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::server.isInitialized) CoroutineScope(Dispatchers.IO).launch {
            try {
                server = ServerSocket(sp.getInt(PageSet.PRF_PORT, -1))
                // close it only on user command and also make it null
                if (m.contacts == null) m.contacts = CopyOnWriteArrayList(dao.contacts())
                listen()
            } catch (_: BindException) {
                // "bind failed: EADDRINUSE (Address already in use)"
            }
        }
        return START_STICKY
    }

    private suspend fun listen() {
        server.accept() // listens until a connection is made (blocks the thread)
            .apply { CoroutineScope(Dispatchers.IO).launch { resolve(this@apply) } }
        listen()
    }

    @Throws(IOException::class, SocketException::class)
    private suspend fun resolve(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream() // don't use PrintWriter even with autoFlush

        // Identify the Transmitter
        val fromIp = socket.remoteSocketAddress.toString().substring(1).split(":")[0]
        val dev = m.radar.devices.find { it.host.hostAddress == fromIp }
        val contact =
            if (dev != null) m.contacts?.find { it.equals(dev) }
            else m.contacts?.find { it.ip == fromIp }

        // Act based on the Header
        val hb = input.read().toByte() // never put "output.read()" in a repeated function!!
        val header = Header.values().find { it.value == hb }
        // Log.println(Log.ASSERT, packageName, "RECEIVER: GOT ${header?.name}")
        val len: Int? = header?.getLength(input.readNBytesCompat(header.indicateLenInNBytes))
        val out: ByteArray = when (header) {
            Header.PAIR ->
                (if (dev != null) findUniqueId(
                    String(input.readNBytesCompat(len!!)),
                    dao.contactIds()
                ).also { chosenId ->
                    Contact.postPairing(this, chosenId, dev)
                    ipToContactId[fromIp] = chosenId
                } else (-1).toShort()).toByteArray()

            Header.INIT -> (if (fromIp in ipToContactId) {
                val chosenId = findUniqueId(String(input.readNBytesCompat(len!!)), dao.chatIds())
                Chat.postInitiation(this, chosenId, ipToContactId[fromIp].toString())
                // FIXME not compatible with group chat
                chosenId
            } else (-1).toShort()).toByteArray()

            Header.TEXT, Header.FILE, Header.COOR -> if (contact != null) {
                decodeMessage(input.readNBytesCompat(len!!).toList(), header, contact).apply {
                    val theChat = dao.chat(chat)
                    val w = try {
                        dao.addMessage(this)
                        val seen = Seen(id, chat, contact.id)
                        dao.addSeen(seen)
                        status = arrayListOf(seen)
                        PageCht.MSG_INSERTED
                    } catch (_: SQLiteConstraintException) {
                        dao.updateMessage(this)
                        matchSeen(dao)
                        PageCht.MSG_UPDATED
                    }
                    when {
                        PageCht.handler != null -> PageCht.handler?.obtainMessage(
                            w, chat.toInt(), if (theChat.muted) 0 else 1, this
                        )?.sendToTarget()
                        Main.handler != null -> Main.handler?.obtainMessage(
                            Main.MSG_NEW_MESSAGE, chat.toInt(), if (theChat.muted) 0 else 1, this
                        )?.sendToTarget()
                        w == PageCht.MSG_INSERTED && !theChat.muted -> notify(contact)
                    }
                }
                0.toByte().toByteArray()
            } else 1.toByte().toByteArray()

            Header.SEEN -> if (contact != null) {
                val raw = input.readNBytesCompat(len!!).toList()
                dao.seen(
                    contact = contact.id,
                    msg = raw.subList(0, 8).toNumber(),
                    chat = raw.subList(8, 10).toNumber(),
                )!!.apply {
                    dateSeen = raw.subList(10, 18).toNumber()
                    dao.updateSeen(this)
                    PageCht.handler?.obtainMessage(PageCht.MSG_SEEN, chat.toInt(), 0, this)
                        ?.sendToTarget()
                }
                0.toByte().toByteArray()
            } else 1.toByte().toByteArray()

            else -> throw IllegalArgumentException("${header?.name}: ${hb.toInt()}")
        }
        output.write(out)
        output.flush()
        socket.close() // necessary for the output stream to send
    }

    private fun findUniqueId(seq: String, ourIds: List<Short>): Short {
        val ids = if (seq.isNotEmpty()) seq.split(",").map { it.toShort() }
            .toMutableSet() else mutableSetOf()
        var chosenId: Short
        ourIds.forEach { ids.add(it) }
        onDestroy()
        do {
            chosenId = (0.toShort()..Short.MAX_VALUE).random().toShort()
        } while (chosenId in ids)
        return chosenId
    }

    private fun decodeMessage(raw: List<Byte>, header: Header, contact: Contact): Message = Message(
        type = header.value,
        from = contact.id,
        id = raw.subList(0, 8).toNumber(),
        chat = raw.subList(8, 10).toNumber(),
        date = raw.subList(10, 18).toNumber(),
        repl = raw.subList(18, 26).toNumber<Long>().let { if (it == -1L) null else it },
        data = String(raw.subList(26, raw.size).toByteArray()),
    )

    private fun Message.notify(contact: Contact) {
        val person = contact.person()
        NotificationManagerCompat.from(c).notify(
            chat.toInt(),
            NotificationCompat.Builder(c, Notify.Channel.NEW_MESSAGE.id).apply {
                setStyle(
                    NotificationCompat.MessagingStyle(person).addMessage(
                        NotificationCompat.MessagingStyle.Message(data, date, person)
                        // FIXME it removes the previous ones
                    )
                )
                setCategory(Notification.CATEGORY_MESSAGE)
                setSmallIcon(R.mipmap.launcher_round)
                setContentIntent(
                    PendingIntent.getActivity(
                        c, 1, Intent(c, Main::class.java)
                            .putExtra(Main.EXTRA_OPEN_CHAT, chat), Notify.mutability(true)
                    )
                )
                setAutoCancel(true)
            }.build()
        )
    }

    override fun onDestroy() {
        m.aliveReceiver = false
        super.onDestroy()
    }

    companion object {
        @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
        @Throws(IOException::class, SocketException::class)
        fun InputStream.readNBytesCompat(len: Int): ByteArray {
            require(len >= 0) { "len < 0" }
            var bufs: MutableList<ByteArray>? = null
            var result: ByteArray? = null
            var total = 0
            var remaining = len
            var n: Int
            do {
                val buf = ByteArray(min(remaining, DEFAULT_BUFFER_SIZE))
                var nread = 0
                while (read(buf, nread, min(buf.size - nread, remaining)).also { n = it } > 0) {
                    nread += n
                    remaining -= n
                }
                if (nread > 0) {
                    if ((Int.MAX_VALUE - 8) - total < nread)
                        throw OutOfMemoryError("Required array size too large")
                    total += nread
                    if (result == null) result = buf
                    else {
                        if (bufs == null) {
                            bufs = ArrayList()
                            bufs.add(result)
                        }
                        bufs.add(buf)
                    }
                }
            } while (n >= 0 && remaining > 0)
            if (bufs == null) {
                if (result == null) return ByteArray(0)
                return if (result.size == total) result else Arrays.copyOf(result, total)
            }
            result = ByteArray(total)
            var offset = 0
            remaining = total
            for (b in bufs) {
                val count = min(b.size, remaining)
                System.arraycopy(b, 0, result, offset, count)
                offset += count
                remaining -= count
            }
            return result
        }

        fun Number.toByteArray(): ByteArray {
            if (this is Byte) return byteArrayOf(this)
            val bb = when (this) {
                is Short -> ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this)
                is Int -> ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this)
                is Long -> ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this)
                else -> throw IllegalArgumentException()
            }
            bb.rewind()
            return bb.array()
        }

        @Suppress("UNCHECKED_CAST")
        fun <N> List<Byte>.toNumber(): N {
            if (size == Byte.SIZE_BYTES) return this[0] as N
            val bb = ByteBuffer.wrap(toByteArray())
            bb.rewind()
            return when (size) {
                Short.SIZE_BYTES -> bb.short as N
                Int.SIZE_BYTES -> bb.int as N
                Long.SIZE_BYTES -> bb.long as N
                else -> 0 as N
            }
        }
    }

    enum class Header(
        val value: Byte, val indicateLenInNBytes: Int, val responseBytes: Int = Byte.SIZE_BYTES
    ) {
        PAIR(0x00, Short.SIZE_BYTES, Short.SIZE_BYTES), // <all Contact ids>
        INIT(0x01, Short.SIZE_BYTES, Short.SIZE_BYTES), // <all Chat ids>

        // Profile picture
        PROF(0x0E, Int.SIZE_BYTES),

        // Seen: <id*4><chat*2><date*4><repl*4><data*n>
        SEEN(0x0F, Byte.SIZE_BYTES),

        // Message: <msg*4><chat*2><dateSeen*4>
        TEXT(0x10, Short.SIZE_BYTES),
        FILE(0x11, Int.SIZE_BYTES),
        COOR(0x12, Byte.SIZE_BYTES);

        fun getLength(ba: ByteArray): Int {
            if (ba.size == Byte.SIZE_BYTES) return ba[0].toInt()
            val bb = ByteBuffer.wrap(ba)
            bb.rewind()
            return when (ba.size) {
                Short.SIZE_BYTES -> bb.short.toInt()
                Int.SIZE_BYTES -> bb.int
                Long.SIZE_BYTES -> bb.long.toInt()
                else -> 0
            }
        }

        fun putLength(size: Int): ByteArray {
            if (indicateLenInNBytes == 1) return byteArrayOf(size.toByte())
            val bb = ByteBuffer.allocate(indicateLenInNBytes)
            when (indicateLenInNBytes) {
                Short.SIZE_BYTES -> bb.putShort(size.toShort())
                Int.SIZE_BYTES -> bb.putInt(size)
                Long.SIZE_BYTES -> bb.putLong(size.toLong())
            }
            bb.rewind()
            return bb.array()
        }
    }
}
