package ir.mahdiparastesh.homechat.data

import android.os.Build
import ir.mahdiparastesh.homechat.BuildConfig
import ir.mahdiparastesh.homechat.util.Time
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class Radar(private val m: Model) : CopyOnWriteArrayList<Radar.Item>() {
    val devices = CopyOnWriteArraySet<Device>()
    var self: Device? = null

    suspend fun insert(item: Device, dao: Database.DAO) {
        item.matchContact(m, dao)
        devices.add(item)
        update(dao)
    }

    suspend fun delete(itemName: String, dao: Database.DAO) {
        devices.forEach {
            if (it.name == itemName) {
                it.contact?.apply {
                    lastOnline = Time.now()
                    dao.updateContact(this)
                }
                try {
                    devices.remove(it)
                } catch (e: java.lang.UnsupportedOperationException) {
                    // mysterious error by CopyOnWriteArrayList$COWIterator.set while sorting
                    if (BuildConfig.DEBUG) throw e
                }
                // NEVER CAST "removeAll {}" on a CopyOnWriteArrayList/Set!!
                // removeAll {} -> filterInPlace() -> iterator() -> CopyOnWriteArrayList$COWIterator::remove()
            }
        }
        update(dao)
    }

    val updateListeners = ArrayList<OnUpdateListener>()

    suspend fun update(dao: Database.DAO) {
        devices.forEach { it.matchContact(m, dao) }
        m.chats?.forEach { chat -> chat.matchContacts(m.contacts!!) }

        // val prev = clone() as CopyOnWriteArrayList<Item>
        clear()
        m.chats?.also { addAll(it) }
        val friends = m.chats?.filter { it.isDirect() }?.map { it.contactIds.toShort() }
        addAll(devices.let { d ->
            if (friends != null) d.filter { it.contact == null || it.contact!!.id !in friends } else d
        })
        sort()
        withContext(Dispatchers.Main) {
            updateListeners.forEach { it.onRadarUpdated() }
        }
    }

    fun sort() {
        try {
            sortBy { it is Chat } // TODO
        } catch (e: java.lang.UnsupportedOperationException) {
            // mysterious error by CopyOnWriteArrayList$COWIterator.set while sorting
            if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) throw e
            // apparently Android 7 (24) adopted JVM 1.8, which supports sorting while altering.
        }
    }

    interface Item

    interface Named {
        fun name(): String
    }

    interface OnUpdateListener {
        fun onRadarUpdated()
    }
}
