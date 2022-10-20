package ir.mahdiparastesh.homechat.more

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model

abstract class WiseService : Service(), Persistent, ViewModelStoreOwner {
    private val mViewModelStore = ViewModelStore()

    override val c: Context get() = applicationContext
    override lateinit var m: Model
    final override val dbLazy: Lazy<Database> = lazy { Database.build(c) }
    override val db: Database by dbLazy
    override val dao: Database.DAO by lazy { db.dao() }

    override fun onCreate() {
        super.onCreate()
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
    }

    override fun onDestroy() {
        if (dbLazy.isInitialized() && m.anyPersistentAlive()) db.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null
    override fun getViewModelStore(): ViewModelStore = mViewModelStore
}
