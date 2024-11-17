package ir.mahdiparastesh.homechat.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.homechat.Sender.Queuable
import java.util.concurrent.CopyOnWriteArrayList

class Model : ViewModel() {
    val radar = Radar(this)
    var contacts: CopyOnWriteArrayList<Contact>? = null
    var chats: CopyOnWriteArrayList<Chat>? = null
    val queue = hashMapOf<Short, ArrayList<Queuable>>()
    var aliveMain = false
    var aliveSender = false
    var aliveReceiver = false

    //fun anyPersistentAlive() = aliveMain || aliveReceiver || aliveSender  // used in Main::onDestroy

    fun enqueue(contact: Short, item: Queuable) {
        if (!queue.containsKey(contact)) queue[contact] = arrayListOf<Queuable>(item)
        else queue[contact]!!.add(item)
    }

    fun dequeue(contact: Short, item: Queuable) {
        if (!queue.containsKey(contact)) return
        queue[contact]!!.remove(item)
        if (queue[contact]!!.isEmpty()) queue.remove(contact)
    }


    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(Model::class.java)) {
                val key = "Model"
                return if (hashMapViewModel.containsKey(key)) getViewModel(key) as T
                else {
                    addViewModel(key, Model())
                    getViewModel(key) as T
                }
            }
            throw IllegalArgumentException("Unknown Model class")
        }

        companion object {
            val hashMapViewModel = HashMap<String, ViewModel>()

            fun addViewModel(key: String, viewModel: ViewModel) =
                hashMapViewModel.put(key, viewModel)

            fun getViewModel(key: String): ViewModel? = hashMapViewModel[key]
        }
    }
}
