package ir.mahdiparastesh.homechat.page

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
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
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent

class PageCht : BasePage<Main>() {
    lateinit var b: PageChtBinding
    lateinit var chat: Chat
    private var replyingTo: Message? = null

    override fun rv(): RecyclerView? = if (::b.isInitialized) b.list else null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onDestChanged = NavController.OnDestinationChangedListener { _, _, _ ->
            c.m.messages = null
        }
        arguments?.getShort(ARG_CHAT_ID)?.let { id -> c.m.chats?.find { it.id == id } }
            .also { if (it != null) chat = it }
        if (!::chat.isInitialized) {
            c.nav.navigateUp(); return; }
        super.onViewCreated(view, savedInstanceState)

        // Load data
        if (c.m.messages == null) CoroutineScope(Dispatchers.IO).launch {
            c.m.messages = ArrayList(c.dao.messages(chat.id)).onEach { it.matchSeen(c.dao) }
            withContext(Dispatchers.Main) { updateList() }
        }

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                if (msg.arg1 != chat.id.toInt() && msg.obj is Message) {
                    Main.handler?.obtainMessage(Main.MSG_NEW_MESSAGE, msg.arg1, msg.arg2, msg.obj)
                        ?.sendToTarget(); return; }
                when (msg.what) {
                    MSG_INSERTED -> (msg.obj as Message).apply {
                        c.m.messages?.also { list ->
                            list.add(this)
                            b.list.adapter?.notifyItemInserted(list.size - 1)
                            b.list.scrollToPosition(list.size - 1)
                        }
                    }
                    MSG_UPDATED -> (msg.obj as Message).apply {
                        c.m.messages?.also { list ->
                            val index = list.indexOfFirst { it.id == id }
                            if (index != -1) {
                                list[index] = this
                                b.list.adapter?.notifyItemChanged(index)
                                b.list.scrollToPosition(list.size - 1)
                            }
                        }
                    }
                    MSG_SEEN -> (msg.obj as Seen).apply {
                        c.m.messages?.also { list ->
                            val index = list.indexOfFirst { it.id == this@apply.msg }
                            if (index != -1) {
                                if (list[index].status == null) list[index].status = arrayListOf()
                                else list[index].status?.removeAll { it.contact == this@apply.contact }
                                list[index].status?.add(this@apply)
                                b.list.adapter?.notifyItemChanged(index)
                                b.list.scrollToPosition(list.size - 1)
                            }
                        }
                    }
                }
            }
        }

        // Reply
        b.replyCancel.setOnClickListener { reply(null) }

        // Field
        b.field.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                canSend(s?.isNotBlank() != false)
            }
        })

        // Send
        canSend(false)
        b.send.setOnClickListener {
            if (chat.contacts == null) return@setOnClickListener
            val text = b.field.text.toString().trim()
            if (text.isBlank()) return@setOnClickListener
            b.field.setText("")
            CoroutineScope(Dispatchers.IO).launch {
                val ids = c.m.messages!!.map { it.id }
                var chosenId = 0L
                do chosenId++ while (chosenId in ids)
                val msg = Message(
                    chosenId, chat.id, Chat.ME, Receiver.Header.TEXT.value, text, replyingTo?.id
                )
                c.dao.addMessage(msg)
                for (contact in chat.contacts!!) Seen(chosenId, chat.id, contact.id).apply {
                    c.dao.addSeen(this)
                    if (msg.status == null) msg.status = arrayListOf()
                    msg.status!!.add(this)
                } // Do not queue the Seen now! It'll be created automatically on the target device!
                c.m.messages?.add(msg)
                withContext(Dispatchers.Main) {
                    Sender.init(c) { putExtra(Sender.EXTRA_NEW_QUEUE, msg.toQueue(c.m)) }
                    c.m.messages?.size?.also { size ->
                        b.list.adapter?.notifyItemInserted(size - 1)
                        b.list.scrollToPosition(size - 1)
                    }
                }
            }
        }

        // Miscellaneous
        KeyboardVisibilityEvent.setEventListener(c, this) {
            // b.list.addOnLayoutChangeListener
            if (!b.list.canScrollVertically(1))
                c.m.messages?.size?.also { size -> b.list.scrollToPosition(size - 1) }
        }
        NotificationManagerCompat.from(c.c).cancel(chat.id.toInt())
    }

    override fun tbTitle(): String = chat.title()

    @SuppressLint("NotifyDataSetChanged")
    private fun updateList() {
        if (b.list.adapter == null) b.list.adapter = ListMsg(c, this)
        else b.list.adapter?.notifyDataSetChanged()
        c.m.messages?.size?.also { size -> b.list.scrollToPosition(size - 1) }
    }

    /*override fun onListScrolled() {
        super.onListScrolled()
    }*/

    private fun canSend(bb: Boolean) {
        b.send.isClickable = bb
        b.send.alpha = if (bb) 1f else 0.8f
    }

    fun reply(replyingTo: Message? = null) {
        this.replyingTo = replyingTo
        val bb = replyingTo != null
        b.reply.isVisible = bb
        b.replyingToContact.text =
            if (bb) getString(
                R.string.replyingTo,
                if (replyingTo.auth == Chat.ME) getString(R.string.yourself)
                else c.m.contacts?.find { it.id == replyingTo.auth }?.name()
            )
            else ""
        b.replyingToMessage.text =
            if (bb) replyingTo.shorten()
            else ""
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }

    companion object {
        const val ARG_CHAT_ID = "chat_id"
        const val MSG_INSERTED = 0
        const val MSG_UPDATED = 1
        const val MSG_SEEN = 2
        var handler: Handler? = null
    }
}
