package ir.mahdiparastesh.homechat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.more.Persistent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
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

    companion object {
        const val EXTRA_PORT = "port"
    }

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
        if (server.isClosed) return
        //try {
        socket = server.accept() // listens until a connection is made (blocks the thread)
        /*} catch (e: SocketException) {
            Main.handler?.obtainMessage(3, e.message.toString())?.sendToTarget()
        }*/ // Socket closed
        val output = socket.getInputStream()
        val hb = output.read().toByte() // never put "output.read()" in a repeated function!!
        val header = Header.values().find { it.value == hb }
        var len: Int? = null
        if (header != null) {
            len = output.readNBytesCompat(header.indicateLenInNBytes).toInt()
        }
        when (header) {
            Header.PAIR -> {
                val ids = String(output.readNBytesCompat(len!!)).split(",")
                    .map { it.toShort() }.toMutableSet()
                db.dao().contactIds().forEach { ids.add(it) }
                Main.handler?.obtainMessage(
                    3, socket.remoteSocketAddress?.toString() + ": " + ids.joinToString(",")
                )?.sendToTarget()
            }
            Header.TEXT -> {
                val msg = output.readNBytesCompat(len!!)
                Main.handler?.obtainMessage(3, String(msg))?.sendToTarget()
            }
            Header.FILE -> {
            }
            else -> {
            }
        }
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

    private fun ByteArray.toInt(): Int {
        var result = 0
        var shift = 0
        for (byte in this) {
            result = result or (byte.toInt() shl shift)
            shift += 8
        }
        return result
    }

    enum class Header(val value: Byte, val indicateLenInNBytes: Int) {
        PAIR(0x00, 1),
        TEXT(0x0A, 1),
        FILE(0x0B, 3),
        COOR(0x0C, 1),
    }
}
