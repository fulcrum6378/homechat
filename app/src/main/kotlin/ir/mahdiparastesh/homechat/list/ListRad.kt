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
import ir.mahdiparastesh.homechat.databinding.ListDevBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder
import ir.mahdiparastesh.homechat.page.PageCht
import java.nio.ByteBuffer

class ListRad(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListDevBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListDevBinding> =
        AnyViewHolder(ListDevBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListDevBinding>, i: Int) {
        val item = c.m.radar.getOrNull(i) ?: return
        val chat = if (item is Chat) item else null
        val dev = if (item is Device) item else null
        h.b.title.text = "${i + 1}. " +
                (dev?.name ?: chat!!.name ?: chat!!.contacts?.firstOrNull()?.name.toString())
        h.b.subtitle.text = dev?.toString() ?: chat?.dateInit?.toString()

        if (chat != null) h.b.root.setOnClickListener {
            c.nav.navigate(
                R.id.action_page_rad_to_page_thd,
                bundleOf(PageCht.ARG_CHAT_ID to chat.id.toString())
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
        val address = toString().makeAddressPair()
        Transmitter(address, Radio.Header.PAIR, Short.SIZE_BYTES, {
            c.dao.contactIds().joinToString(",").encodeToByteArray()
        }) { res ->
            if (res == null) {
                return@Transmitter; }
            Contact.postPairing(c, ByteBuffer.wrap(res).short, this)
        }
    }

    override fun getItemCount(): Int = c.m.radar.size
}
