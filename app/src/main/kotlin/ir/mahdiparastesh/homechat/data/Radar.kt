package ir.mahdiparastesh.homechat.data

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

class Radar(private val m: Model) : CopyOnWriteArrayList<Radar.Item>() {
    val devices = CopyOnWriteArraySet<Device>()
    var self: Device? = null

    fun insert(item: Device) {
        item.matchContact(m.contacts)
        devices.add(item)
        update()
    }

    fun delete(itemName: String) {
        devices.forEach { if (it.name == itemName) devices.remove(it) }
        // NEVER CAST "removeAll {}" on a CopyOnWriteArrayList/Set!!
        // removeAll {} -> filterInPlace() -> iterator() -> CopyOnWriteArrayList$COWIterator::remove()
        update()
    }

    var onDataChangedListener: () -> Unit = {}

    //@Suppress("UNCHECKED_CAST")
    fun update() {
        devices.onEach { it.matchContact(m.contacts) }
        m.chats?.onEach { chat ->
            chat.contacts = chat.contactIds.split(Chat.CONTACT_SEP)
                .map { id -> m.contacts!!.find { it.id == id.toShort() } }
        }

        // val prev = clone() as CopyOnWriteArrayList<Item>
        clear()
        m.chats?.also { addAll(it) }
        val friends = m.chats?.filter { it.isDirect() }?.map { it.contactIds.toShort() }
        addAll(devices.let { d ->
            if (friends != null) d.filter { it.contact == null || it.contact!!.id !in friends } else d
        })
        sortBy { it is Chat }
        onDataChangedListener()
    }

    interface Item
}
