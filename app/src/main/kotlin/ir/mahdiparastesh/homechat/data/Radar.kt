package ir.mahdiparastesh.homechat.data

import java.util.concurrent.CopyOnWriteArrayList

class Radar(private val m: Model) : CopyOnWriteArrayList<Radar.Item>() {

    fun insert(item: Device) {
        add(item)
        onEach { if (it is Device) it.matchContact(m.contacts) }
        onInnerChangeListener()
    }

    fun delete(itemName: String) {
        removeAll { it is Device && it.name == itemName }
        onInnerChangeListener()
    }

    var onInnerChangeListener: () -> Unit = {}

    fun onOuterChange() {
        /*val newRadar = m.radar.value?.onEach { it.matchContact(m.contacts) }
            withContext(Dispatchers.Main) { m.radar.value = newRadar }*/
        /*matchContact(c.m.contacts)
                c.m.radar.value = c.m.radar.value*/
    }

    interface Item {
        var name: String
    }
}
