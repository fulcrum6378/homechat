package ir.mahdiparastesh.homechat.page

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Sender
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Message
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.databinding.PageChtBinding
import ir.mahdiparastesh.homechat.list.ListMsg
import ir.mahdiparastesh.homechat.more.BasePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PageCht : BasePage<Main>() {
    private lateinit var b: PageChtBinding
    lateinit var chat: Chat

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onDestChanged = NavController.OnDestinationChangedListener { _, _, _ ->
            c.m.messages = null
        }
        super.onViewCreated(view, savedInstanceState)
        arguments?.getShort(ARG_CHAT_ID)?.let { id -> c.m.chats?.find { it.id == id } }.also {
            if (it == null) {
                c.nav.navigateUp(); return; }
            chat = it
        }

        if (c.m.messages == null) CoroutineScope(Dispatchers.IO)
            .launch { c.m.messages = ArrayList(c.dao.messages(chat.id)) }
            .invokeOnCompletion { updateList() }

        b.send.setOnClickListener {
            if (chat.contacts == null) return@setOnClickListener
            val text = b.field.text.toString()
            val repl: Long? = null
            b.field.setText("")
            CoroutineScope(Dispatchers.IO).launch {
                val ids = c.m.messages!!.map { it.id }
                var chosenId = 0L
                do chosenId++ while (chosenId in ids)
                val msg = Message(
                    chosenId, chat.id, Chat.ME, Receiver.Header.TEXT.value, text, repl
                )
                c.dao.addMessage(msg)
                var sq = arrayOf<String>()
                chat.contacts!!.forEach { contact ->
                    Seen(chosenId, chat.id, contact.id).apply {
                        c.dao.addSeen(this)
                        if (msg.status == null) msg.status = arrayListOf()
                        msg.status!!.add(this)
                        sq = sq.plus(toQueue(c.m))
                    }
                }
                c.m.messages?.add(msg)
                withContext(Dispatchers.Main) {
                    updateList() // FIXME use notifyAdded
                    Sender.init(c) { putExtra(Sender.EXTRA_NEW_QUEUE, msg.toQueue(c.m).plus(sq)) }
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateList() {
        if (b.list.adapter == null) b.list.adapter = ListMsg(c, this)
        else b.list.adapter?.notifyDataSetChanged()
    }

    companion object {
        const val ARG_CHAT_ID = "chat_id"
    }
}
