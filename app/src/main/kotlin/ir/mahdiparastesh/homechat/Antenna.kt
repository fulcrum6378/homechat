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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class Antenna : Service(), Persistent, ViewModelStoreOwner {
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
        if (!::server.isInitialized) Thread { // one-time socket!!
            server = ServerSocket(intent.getIntExtra(EXTRA_PORT, 0))
            receive()
        }.start()
        return START_NOT_STICKY
    }

    private fun receive() {
        try {
            socket = server.accept() // listens until a connection is made (blocks the thread)
        } catch (e: SocketException) {
            Main.handler?.obtainMessage(3, e.message.toString())?.sendToTarget()
        } // Socket closed
        BufferedReader(InputStreamReader(socket.getInputStream())).readText()
            .also { Main.handler?.obtainMessage(3, it)?.sendToTarget() }
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

    enum class Header(value: Byte) {
        PAIR(0x00),
        TEXT(0x0A),
    }
}
