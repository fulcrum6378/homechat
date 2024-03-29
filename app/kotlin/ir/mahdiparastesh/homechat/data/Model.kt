package ir.mahdiparastesh.homechat.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.CopyOnWriteArrayList

class Model : ViewModel() {
    val radar = Radar(this)
    var contacts: CopyOnWriteArrayList<Contact>? = null
    var chats: CopyOnWriteArrayList<Chat>? = null
    var messages: ArrayList<Message>? = null
    var aliveMain = false
    var aliveSender = false
    var aliveReceiver = false

    fun anyPersistentAlive() = aliveMain || aliveReceiver || aliveSender

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
