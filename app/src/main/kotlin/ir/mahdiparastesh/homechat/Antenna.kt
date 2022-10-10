package ir.mahdiparastesh.homechat

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Model
import java.net.ServerSocket

class Antenna : Service(), ViewModelStoreOwner {
    private val c: Context get() = applicationContext
    private val mViewModelStore = ViewModelStore()
    private lateinit var m: Model
    private lateinit var server: ServerSocket

    override fun onCreate() {
        super.onCreate()
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!::server.isInitialized) {
            server = ServerSocket(intent.getIntExtra("PORT", 0))
            Log.println(Log.ASSERT, "TRIJNTJE", "Server opened at ${server.localPort}...")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server.close()
        Log.println(Log.ASSERT, "TRIJNTJE", "Server ${server.localPort} closed!")
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun getViewModelStore(): ViewModelStore = mViewModelStore
}
