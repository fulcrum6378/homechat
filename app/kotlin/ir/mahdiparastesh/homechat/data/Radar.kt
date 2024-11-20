package ir.mahdiparastesh.homechat.data

import android.os.Build
import ir.mahdiparastesh.homechat.BuildConfig
import ir.mahdiparastesh.homechat.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class Radar(private val m: Model) : CopyOnWriteArrayList<Radar.Item>() {
    val devices = HashMap<String, Device>()
    val onlineIPs = CopyOnWriteArraySet<String>()
    var self: Device? = null

    suspend fun insert(item: Device, dao: Database.DAO) {
        item.matchContact(m, dao)
        devices[item.name] = item
        update(dao)
    }

    suspend fun delete(name: String, dao: Database.DAO) {
        devices.remove(name)
        update(dao)
    }

    val updateListeners = ArrayList<OnUpdateListener>()

    suspend fun update(dao: Database.DAO) {
        devices.values.forEach { it.matchContact(m, dao) }
        m.chats?.forEach { chat -> chat.matchContacts(m.contacts!!) }

        onlineIPs.clear()
        onlineIPs.addAll(devices.values.map { it.host.hostAddress })

        clear()
        m.chats?.also { addAll(it) }
        val friends = m.chats?.filter { it.isDirect() }?.map { it.contact_ids.toShort() }
        addAll(if (friends != null)
            devices.values.filter { it.contact == null || it.contact!!.id !in friends }
        else devices.values)
        sort()
        withContext(Dispatchers.Main) {
            updateListeners.forEach { it.onRadarUpdated() }
        }
    }

    fun sort() {
        try {
            sortWith(compareBy({ it is Chat }, { it is Chat && !it.pinned }))
        } catch (e: java.lang.UnsupportedOperationException) {
            // mysterious error by CopyOnWriteArrayList$COWIterator.set while sorting
            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) throw e
            // apparently Android 7 (24) adopted JVM 1.8, which supports sorting while altering.
        }
    }

    suspend fun shutdown(dao: Database.DAO) {
        val now = Time.now()
        devices.values.forEach {
            it.contact?.apply {
                online_at = now
                dao.updateContact(this)
            }
        }
        devices.clear()
        update(dao)
    }

    interface Item

    interface Named {
        fun name(): String
    }

    interface OnUpdateListener {
        fun onRadarUpdated()
    }
}
