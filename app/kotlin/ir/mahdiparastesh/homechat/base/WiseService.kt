package ir.mahdiparastesh.homechat.base

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model

abstract class WiseService : Service(), Persistent, ViewModelStoreOwner {
    override val c: Context get() = applicationContext
    override lateinit var m: Model
    final override val dbLazy: Lazy<Database> = lazy { database() }
    override val db: Database by dbLazy
    override val dao: Database.DAO by lazy { db.dao() }
    override lateinit var sp: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        m = model()
        sp = sp()
    }

    /*override fun onDestroy() {
        // if (dbLazy.isInitialized() && m.anyPersistentAlive()) db.close()
        super.onDestroy()
    }*/

    override fun onBind(intent: Intent): IBinder? = null
    override val viewModelStore: ViewModelStore = ViewModelStore()
}
