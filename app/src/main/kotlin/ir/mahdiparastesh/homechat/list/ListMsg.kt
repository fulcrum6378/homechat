package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.databinding.ListMsgBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder
import ir.mahdiparastesh.homechat.page.PageCht

class ListMsg(private val c: Main/*, private val f: PageCht*/) :
    RecyclerView.Adapter<AnyViewHolder<ListMsgBinding>>() {

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListMsgBinding> =
        AnyViewHolder(ListMsgBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListMsgBinding>, i: Int) {
        val msg = c.m.messages?.getOrNull(i) ?: return
    }

    override fun getItemCount(): Int = c.m.radar.size
}
