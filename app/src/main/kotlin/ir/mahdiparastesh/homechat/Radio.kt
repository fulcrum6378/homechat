package ir.mahdiparastesh.homechat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.more.Persistent
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
class Radio : Service(), Persistent, ViewModelStoreOwner {
    private val mViewModelStore = ViewModelStore()
    private lateinit var server: ServerSocket
    private lateinit var socket: Socket

    override val c: Context get() = applicationContext
    override lateinit var m: Model
    override val dbLazy: Lazy<Database> = lazy { Database.build(c) }
    override val db: Database by dbLazy
    override val dao: Database.DAO by lazy { db.dao() }

    override fun onCreate() {
        super.onCreate()
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
        m.aliveAntenna = true
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
        val dev: Device = m.radar.find { it is Device && it.host.hostAddress == fromIp } as Device//?
        // TODO if (dev == null)

        // Act based on the Header
        val hb = input.read().toByte() // never put "output.read()" in a repeated function!!
        val header = Header.values().find { it.value == hb }
        val len: Int? = header?.get(input.readNBytesCompat(header.indicateLenInNBytes))
        when (header) {
            Header.PAIR -> {
                val ids = String(input.readNBytesCompat(len!!)).split(",")
                    .map { it.toShort() }.toMutableSet()
                var chosenId: Short
                dao.contactIds().forEach { ids.add(it) }
                do {
                    chosenId = (0..Short.MAX_VALUE).random().toShort()
                } while (chosenId in ids)
                Contact(
                    chosenId, dev.name, fromIp, Database.now(),
                    dev.email, dev.phone, Database.now()
                ).also {
                    dao.addContact(it)
                    m.contacts?.add(it)
                    m.radar.onOuterChange()
                }
                output.write(
                    (ByteBuffer.allocate(Short.SIZE_BYTES)
                        .putShort(chosenId).rewind() as ByteBuffer).array()
                )
                output.flush()
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

    override fun onDestroy() {
        if (::server.isInitialized) server.close()
        m.aliveAntenna = false
        if (dbLazy.isInitialized() && m.anyPersistentAlive()) db.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null
    override fun getViewModelStore(): ViewModelStore = mViewModelStore

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
        PAIR(0x00, Byte.SIZE_BYTES),
        TEXT(0x0A, Byte.SIZE_BYTES),
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
            bb.putInt(size)
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
