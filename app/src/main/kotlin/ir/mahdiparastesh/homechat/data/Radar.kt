package ir.mahdiparastesh.homechat.data

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
        devices.forEach { if (it.name == itemName) devices.remove(it) }
        // NEVER CAST "removeAll {}" on a CopyOnWriteArrayList/Set!!
        // removeAll {} -> filterInPlace() -> iterator() -> CopyOnWriteArrayList$COWIterator::remove()
        update(dao)
    }

    var onDataChangedListener: () -> Unit = {}

    @Suppress(/*"UNCHECKED_CAST",*/ "RedundantSuspendModifier")
    suspend fun update(dao: Database.DAO) {
        devices.forEach { it.matchContact(m, dao) }
        m.chats?.forEach { chat ->
            chat.contacts = chat.contactIds.split(Chat.CONTACT_SEP)
                .map { id -> m.contacts!!.find { it.id == id.toShort() }!! }
        }

        // val prev = clone() as CopyOnWriteArrayList<Item>
        clear()
        m.chats?.also { addAll(it) }
        val friends = m.chats?.filter { it.isDirect() }?.map { it.contactIds.toShort() }
        addAll(devices.let { d ->
            if (friends != null) d.filter { it.contact == null || it.contact!!.id !in friends } else d
        })
        sortBy { it is Chat }
        withContext(Dispatchers.Main) { onDataChangedListener() }
    }

    interface Item

    interface Named {
        fun name(): String
    }
}
