package ir.mahdiparastesh.homechat.page

import android.annotation.SuppressLint
import android.content.Intent
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
import ir.mahdiparastesh.homechat.Sender
import ir.mahdiparastesh.homechat.base.BasePage
import ir.mahdiparastesh.homechat.data.Binary
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Message
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.databinding.PageChtBinding
import ir.mahdiparastesh.homechat.list.ListMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PageCht : BasePage<Main>() {
    lateinit var b: PageChtBinding
    lateinit var chat: Chat
    private var replyingTo: Message? = null
    private val binCacheDir by lazy { "${c.cacheDir.absolutePath}/binary/" }

    override fun rv(): RecyclerView? = if (::b.isInitialized) b.list else null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    companion object {
        const val ARG_CHAT_ID = "chat_id"
        const val MSG_INSERTED = 0
        const val MSG_SEEN = 1
        var handler: Handler? = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onDestChanged = NavController.OnDestinationChangedListener { _, _, _ ->
            c.mm.messages = null
        }
        arguments?.getShort(ARG_CHAT_ID)?.let { id -> c.m.chats?.find { it.id == id } }
            .also { if (it != null) chat = it }
        if (!::chat.isInitialized) {
            c.nav.navigateUp(); return; }
        super.onViewCreated(view, savedInstanceState)

        // Load data
        if (c.mm.messages == null) CoroutineScope(Dispatchers.IO).launch {
            c.mm.messages = ArrayList(c.dao.messages(chat.id)).onEach {
                it.querySeen(c.dao)
                it.queryBinaries(c.dao)
            }
            withContext(Dispatchers.Main) { updateList() }
        }

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                when (msg.what) {
                    MSG_INSERTED -> (msg.obj as Message).apply {
                        c.mm.messages?.add(this)
                        b.list.adapter?.notifyItemInserted(c.mm.messages?.size?.let { it - 1 } ?: 0)
                    }
                    MSG_SEEN -> (msg.obj as Seen).apply {
                        val index = c.mm.messages?.indexOfFirst {
                            it.id == this@apply.msg && it.auth == this@apply.auth
                        } ?: return@apply
                        if (index != -1) {
                            c.mm.messages!![index].saw(this@apply)
                            b.list.adapter?.notifyItemChanged(index)
                        }
                    }
                }
            }
        }

        // Reply
        b.replyCancel.setOnClickListener { reply(null) }

        // Attach
        val bcdf = File(binCacheDir)
        if (!bcdf.exists()) bcdf.mkdir()
        b.attach.setOnClickListener {
            c.launch.pageCht = this
            c.launch.attach.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
            )
        }

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
                Message(
                    findUniqueId(), chat.id, Chat.ME, Message.Type.TEXT.value, text, replyingTo?.id
                ).send()
            }
        }

        // Miscellaneous
        KeyboardVisibilityEvent.setEventListener(c, this) {
            // b.list.addOnLayoutChangeListener
            if (!b.list.canScrollVertically(1))
                c.mm.messages?.size?.also { size -> b.list.scrollToPosition(size - 1) }
        }
        NotificationManagerCompat.from(c.c).cancel(chat.id.toInt())
    }

    override fun tbTitle(): String = chat.title()

    @SuppressLint("NotifyDataSetChanged")
    private fun updateList() {
        if (b.list.adapter == null) b.list.adapter = ListMsg(c, this)
        else b.list.adapter?.notifyDataSetChanged()
        c.mm.messages?.size?.also { size -> b.list.scrollToPosition(size - 1) }
    }

    /*override fun onListScrolled() {
        super.onListScrolled()
    }*/

    private fun canSend(bb: Boolean) {
        b.send.isVisible = bb
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

    suspend fun attach(intent: Intent) {
        val fd = intent.data?.let { c.contentResolver.openFileDescriptor(it, "r") } ?: return

        val msgId = findUniqueId()
        val bin = Binary(
            fd.statSize, c.contentResolver.getType(intent.data!!), intent.dataString,
            0.toByte(), msgId, chat.id, Chat.ME
        )
        val binId = c.dao.addBinary(bin)
        bin.id = binId  // required for enqueue -> toQueue

        Message(
            msgId, chat.id, Chat.ME, Message.Type.FILE.value, binId.toString(),
            replyingTo?.id
        ).apply {
            binaries = listOf(bin)
        }.send()

        for (contact in this@PageCht.chat.contacts!!)
            c.m.enqueue(contact.id, bin)

        // cache the file
        FileInputStream(fd.fileDescriptor).use { fis ->
            FileOutputStream(binCacheDir + binId.toString()).use { fos ->
                fis.copyTo(fos)
            }
        }
        fd.close()
    }

    private fun findUniqueId(): Long {
        val ids = c.mm.messages!!.filter { it.auth == Chat.ME }.map { it.id }
        var chosenId = 0L
        do chosenId++ while (chosenId in ids)
        return chosenId
    }

    private suspend fun Message.send() {
        c.dao.addMessage(this)
        c.mm.messages?.add(this)
        for (contact in this@PageCht.chat.contacts!!) {
            c.m.enqueue(contact.id, this)

            val seen = Seen(id, this@PageCht.chat.id, Chat.ME, contact.id)
            c.dao.addSeen(seen)
            saw(seen)
            // Do not queue the Seen now! It'll be created automatically on the target device!
        }

        withContext(Dispatchers.Main) {
            Sender.init(c)
            c.mm.messages?.size?.also { size ->
                b.list.adapter?.notifyItemInserted(size - 1)
                b.list.scrollToPosition(size - 1)
            }
            reply(null)
        }
    }

    override fun onDestroy() {
        handler = null
        super.onDestroy()
    }
}
