package ir.mahdiparastesh.homechat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Model
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class Antenna : Service(), ViewModelStoreOwner {
    // private val c: Context get() = applicationContext
    private val mViewModelStore = ViewModelStore()
    private lateinit var m: Model
    private lateinit var server: ServerSocket
    private lateinit var socket: Socket

    override fun onCreate() {
        super.onCreate()
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!::server.isInitialized) Thread { // one-time socket!!
            server = ServerSocket(intent.getIntExtra("port", 0))
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
        super.onDestroy()
        if (::server.isInitialized) server.close()
    }

    override fun onBind(intent: Intent): IBinder? = null
    override fun getViewModelStore(): ViewModelStore = mViewModelStore

    enum class Header(value: Byte) {
        HANDSHAKE(0x00),
        TEXT(0x0A),
    }
}
