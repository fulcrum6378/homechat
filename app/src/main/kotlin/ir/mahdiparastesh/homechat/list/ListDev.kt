package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.databinding.ListDevBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder

class ListDev(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListDevBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListDevBinding> =
        AnyViewHolder(ListDevBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListDevBinding>, i: Int) {
        val dev = c.m.radar.value!!.getOrNull(i) ?: return
        h.b.name.text = dev.name
        h.b.address.text = dev.toString() // replace with the recent chat or online status
        h.b.root.setOnClickListener {
            c.nav.navigate(R.id.action_page_rad_to_page_thd, bundleOf("device" to dev.toString()))
        }
    }

    override fun getItemCount(): Int =
        c.m.radar.value?.size ?: 0 //?.let { if (it.size < 2) 0 else it.size - 1 } ?: 0
}
