package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.Radio
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.databinding.ListDevBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder
import ir.mahdiparastesh.homechat.page.PageCht
import java.nio.ByteBuffer

class ListDev(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListDevBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListDevBinding> =
        AnyViewHolder(ListDevBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListDevBinding>, i: Int) {
        val dev = c.m.radar.getOrNull(i) ?: return
        h.b.name.text = dev.name
        h.b.address.text = dev.contact?.id?.toString() ?: "-"
        h.b.root.setOnClickListener {
            if (dev.contact == null) MaterialAlertDialogBuilder(c).apply {
                setTitle(R.string.newContact)
                setPositiveButton(R.string.pair) { _, _ -> dev.pair() }
                setNegativeButton(R.string.cancel, null)
            } else c.nav.navigate(
                R.id.action_page_rad_to_page_thd,
                bundleOf(PageCht.ARG_DEVICE to dev.toString())
            )
        }
    }

    private fun Device.pair() {
        val address = toString().makeAddressPair()
        Transmitter(address, Radio.Header.PAIR, Short.SIZE_BYTES, {
            c.dao.contactIds().joinToString(",").encodeToByteArray()
        }) { res ->
            if (res == null) {
                return@Transmitter; }
            Contact(
                ByteBuffer.wrap(res).short, name, address.first, Database.now(),
                email, phone, Database.now()
            ).also {
                c.dao.addContact(it)
                c.m.contacts?.add(it)
                c.m.radar.onOuterChange()
            }
        }
    }

    override fun getItemCount(): Int = c.m.radar.size
}
