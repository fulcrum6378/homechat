package ir.mahdiparastesh.homechat.page

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.databinding.PageRadBinding
import ir.mahdiparastesh.homechat.list.ListRad
import ir.mahdiparastesh.homechat.more.BasePage

class PageRad : BasePage<Main>() {
    private lateinit var b: PageRadBinding

    override fun rv(): RecyclerView? = if (::b.isInitialized) b.list else null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageRadBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        c.m.radar.onDataChangedListener = { updateList() }
        updateList()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateList() {
        if (b.list.isComputingLayout) return
        if (b.list.adapter == null) b.list.adapter = ListRad(c)
        else b.list.adapter?.notifyDataSetChanged()
        val isEmpty = b.list.adapter!!.itemCount == 0
        b.empty.isVisible = isEmpty
        b.list.isVisible = !isEmpty
    }
}
