package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.Radio
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.databinding.ListRadBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder
import ir.mahdiparastesh.homechat.page.PageCht
import java.nio.ByteBuffer

class ListRad(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListRadBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListRadBinding> =
        AnyViewHolder(ListRadBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListRadBinding>, i: Int) {
        val item = c.m.radar.getOrNull(i) ?: return
        val chat = if (item is Chat) item else null
        val dev = if (item is Device) item else null
        h.b.title.text = "${i + 1}. " +
                (dev?.name ?: chat!!.name ?: chat!!.contacts?.firstOrNull()?.name.toString())
        h.b.subtitle.text = dev?.toString() ?: chat?.dateInit?.let { c.dateFormat.format(it) }

        if (chat != null) h.b.root.setOnClickListener {
            c.nav.navigate(
                R.id.action_page_rad_to_page_cht,
                bundleOf(PageCht.ARG_CHAT_ID to chat.id)
            )
        } else if (dev != null) h.b.root.setOnClickListener {
            MaterialAlertDialogBuilder(c).apply {
                setTitle(R.string.newContact)
                setPositiveButton(R.string.pair) { _, _ -> dev.pair() }
                setNegativeButton(R.string.cancel, null)
            }.show()
        }
    }

    private fun Device.pair() {
        Main.handler?.obtainMessage(3, "Device.pair")?.sendToTarget()
        val address = toString().makeAddressPair()
        Transmitter(address, Radio.Header.PAIR, Short.SIZE_BYTES, {
            c.dao.contactIds().joinToString(",").encodeToByteArray()
        }) { res ->
            if (res == null) {
                return@Transmitter; }
            Main.handler?.obtainMessage(3, "contact id chosen")?.sendToTarget()
            Contact.postPairing(c, ByteBuffer.wrap(res).short, this)
                .apply { listOf(this).init(address) }
        }
    }

    private fun List<Contact>.init(address: Pair<String, Int>) {
        Main.handler?.obtainMessage(3, "List<Contact>.init")?.sendToTarget()
        Transmitter(address, Radio.Header.INIT, Short.SIZE_BYTES, {
            c.dao.chatIds().joinToString(",").encodeToByteArray()
        }) { res ->
            if (res == null) {
                return@Transmitter; }
            Main.handler?.obtainMessage(3, "chat id chosen")?.sendToTarget()
            Chat.postInitiation(
                c, ByteBuffer.wrap(res).short, joinToString(Chat.CONTACT_SEP) { it.id.toString() })
        }
    }

    override fun getItemCount(): Int = c.m.radar.size
}
