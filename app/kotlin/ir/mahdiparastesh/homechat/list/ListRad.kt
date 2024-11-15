package ir.mahdiparastesh.homechat.list

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.Receiver
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.base.AnyViewHolder
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.databinding.ListRadBinding
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.util.EasyMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ListRad(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListRadBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListRadBinding> =
        AnyViewHolder(ListRadBinding.inflate(c.layoutInflater, parent, false))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: AnyViewHolder<ListRadBinding>, i: Int) {
        val item = c.m.radar.getOrNull(i) ?: return
        val chat = item as? Chat
        val dev = item as? Device

        // texts
        h.b.title.text = "${i + 1}. " + (dev?.name ?: chat!!.title())
        h.b.subtitle.text = dev?.toString() ?: chat!!.onlineStatus(c)

        // counter badge
        val hasNew = chat?.newOnes != null && chat.newOnes!! > 0
        if (hasNew) h.b.badge.text = chat.newOnes.toString()
        h.b.badge.isVisible = hasNew

        // clicks
        if (chat != null) {
            h.b.root.setOnClickListener {
                c.nav.navigate(
                    R.id.action_page_rad_to_page_cht, bundleOf(PageCht.ARG_CHAT_ID to chat.id)
                )
            }
            h.b.root.setOnLongClickListener {
                EasyMenu(
                    c, it, R.menu.list_rad_chat, hashMapOf(
                        R.id.chat_pin to { chat.edit { pinned = true } },
                        R.id.chat_unpin to { chat.edit { pinned = false } },
                        R.id.chat_mute to { chat.edit { muted = true } },
                        R.id.chat_unmute to { chat.edit { muted = false } },
                    )
                ).show {
                    if (chat.pinned) {
                        menu.findItem(R.id.chat_pin).isVisible = false
                        menu.findItem(R.id.chat_unpin).isVisible = true
                    }
                    if (chat.muted) {
                        menu.findItem(R.id.chat_mute).isVisible = false
                        menu.findItem(R.id.chat_unmute).isVisible = true
                    }
                }
                true
            }
        } else if (dev != null) {
            h.b.root.setOnClickListener {
                MaterialAlertDialogBuilder(c).apply {
                    setTitle(R.string.newContact)
                    setPositiveButton(R.string.pair) { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch { dev.pair() }
                    }
                    setNegativeButton(R.string.cancel, null)
                }.show()
            }
            h.b.root.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = c.m.radar.size

    private suspend fun Device.pair() {
        val address = toString().makeAddressPair()
        Transmitter(address, Receiver.Header.PAIR, {
            c.dao.contactIds().joinToString(",").encodeToByteArray()
        }, { it != null }, {
            withContext(Dispatchers.Main) { error("pair() returned null; using VPN?") }
        }) { res ->
            val chosenId = ByteBuffer.wrap(res!!).short
            if (chosenId == (-1).toShort()) {
                withContext(Dispatchers.Main) { error("chosenId == -1") }
                return@Transmitter; }
            Contact.postPairing(c, chosenId, this)
                .apply { listOf(this).init(address) }
        }
    }

    private suspend fun List<Contact>.init(address: Pair<String, Int>) {
        Transmitter(address, Receiver.Header.INIT, {
            c.dao.chatIds().joinToString(",").encodeToByteArray()
        }, { it != null }, {
            withContext(Dispatchers.Main) { error("init() returned null") }
        }) { res ->
            Chat.postInitiation(
                c, ByteBuffer.wrap(res!!).short,
                joinToString(Chat.CONTACT_SEP) { it.id.toString() })
        }
    }

    private fun error(why: String) {
        Toast.makeText(c, "Pairing failed: $why", Toast.LENGTH_LONG).show()
    }

    private fun Chat.edit(func: Chat.() -> Unit) {
        func()
        CoroutineScope(Dispatchers.IO).launch {
            c.dao.updateChat(this@edit)
            val oldIndex = c.m.radar.indexOf(this@edit)
            c.m.radar.sort()
            val newIndex = c.m.radar.indexOf(this@edit)
            if (oldIndex != newIndex) withContext(Dispatchers.Main) {
                notifyItemMoved(oldIndex, newIndex)
            }
        }
    }
}
