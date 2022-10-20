package ir.mahdiparastesh.homechat

import android.content.Intent
import ir.mahdiparastesh.homechat.data.*
import ir.mahdiparastesh.homechat.more.WiseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.min

@Suppress("BlockingMethodInNonBlockingContext")
class Receiver : WiseService() {
    private lateinit var server: ServerSocket
    private lateinit var socket: Socket
    private val ipToContactId = hashMapOf<String, Short>()

    override fun onCreate() {
        super.onCreate()
        m.aliveReceiver = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!::server.isInitialized) CoroutineScope(Dispatchers.IO).launch { // one-time socket!!
            server = ServerSocket(intent.getIntExtra(EXTRA_PORT, 0))
            receive()
        }.start()
        return START_NOT_STICKY
    }

    private suspend fun receive() {
        try {
            socket = server.accept() // listens until a connection is made (blocks the thread)
        } catch (e: SocketException) {
            if (e.message == "Socket closed") return // this catch is always necessary!
            else Main.handler?.obtainMessage(3, e.message.toString())?.sendToTarget()
        }
        val input = socket.getInputStream()
        val output = socket.getOutputStream() // don't use PrintWriter even with autoFlush

        // Identify the Transmitter
        val fromIp = socket.remoteSocketAddress.toString().substring(1).split(":")[0]
        val dev = m.radar.find { it is Device && it.host.hostAddress == fromIp } as Device?//?
        dev!!// TODO if (dev == null)

        // Act based on the Header
        val hb = input.read().toByte() // never put "output.read()" in a repeated function!!
        val header = Header.values().find { it.value == hb }
        val len: Int? = header?.get(input.readNBytesCompat(header.indicateLenInNBytes))
        when (header) {
            Header.PAIR -> {
                val chosenId = findUniqueId(String(input.readNBytesCompat(len!!)), dao.contactIds())
                Contact.postPairing(this, chosenId, dev)
                ipToContactId[fromIp] = chosenId
                output.write(
                    (ByteBuffer.allocate(Short.SIZE_BYTES)
                        .putShort(chosenId).rewind() as ByteBuffer).array()
                )
                output.flush()
            }
            Header.INIT -> if (fromIp in ipToContactId) {
                val chosenId = findUniqueId(String(input.readNBytesCompat(len!!)), dao.chatIds())
                Chat.postInitiation(this, chosenId, ipToContactId[fromIp].toString())
                // FIXME not compatible with group chat
                output.write(
                    (ByteBuffer.allocate(Short.SIZE_BYTES)
                        .putShort(chosenId).rewind() as ByteBuffer).array()
                )
                output.flush()
            } else {
                // TODO NULL
            }
            Header.TEXT -> {
                val msg = input.readNBytesCompat(len!!)
                Main.handler?.obtainMessage(3, String(msg))?.sendToTarget()
            }
            Header.FILE -> {
            }
            else -> {
            }
        }
        socket.close() // necessary for the output stream to send
        receive()
    }

    private fun findUniqueId(seq: String, ourIds: List<Short>): Short {
        val ids = if (seq.isNotEmpty()) seq.split(",").map { it.toShort() }
            .toMutableSet() else mutableSetOf()
        var chosenId: Short
        ourIds.forEach { ids.add(it) }
        do {
            chosenId = (0..Short.MAX_VALUE).random().toShort()
        } while (chosenId in ids)
        return chosenId
    }

    override fun onDestroy() {
        if (::server.isInitialized) server.close()
        m.aliveReceiver = false
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PORT = "port"

        @Suppress("EXTENSION_SHADOWED_BY_MEMBER")
        @Throws(IOException::class)
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
    }

    enum class Header(val value: Byte, val indicateLenInNBytes: Int) {
        PAIR(0x00, Short.SIZE_BYTES), // <all Contact ids>
        INIT(0x01, Short.SIZE_BYTES), // <all Chat ids>
        // Message
        TEXT(0x0A, Short.SIZE_BYTES),
        FILE(0x0B, Int.SIZE_BYTES),
        COOR(0x0C, Byte.SIZE_BYTES);

        fun get(ba: ByteArray): Int {
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

        fun put(size: Int): ByteArray {
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
