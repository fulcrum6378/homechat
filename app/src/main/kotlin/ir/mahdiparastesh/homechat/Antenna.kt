package ir.mahdiparastesh.homechat

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Model
import java.net.ServerSocket

class Antenna : Service(), ViewModelStoreOwner {
    private val mViewModelStore = ViewModelStore() // LifecycleService is not a ViewModelStoreOwner
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
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        server.close()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun getViewModelStore(): ViewModelStore = mViewModelStore
}
