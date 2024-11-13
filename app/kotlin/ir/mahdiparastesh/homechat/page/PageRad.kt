package ir.mahdiparastesh.homechat.page

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.base.BasePage
import ir.mahdiparastesh.homechat.data.Radar.OnUpdateListener
import ir.mahdiparastesh.homechat.databinding.PageRadBinding
import ir.mahdiparastesh.homechat.list.ListRad

class PageRad : BasePage<Main>(), OnUpdateListener {
    private lateinit var b: PageRadBinding

    override fun rv(): RecyclerView? = if (::b.isInitialized) b.list else null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageRadBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onResume() {
        super.onResume()
        updateList()
        c.m.radar.updateListeners.add(this)
    }

    override fun tbTitle(): String = getString(R.string.app_name)

    @SuppressLint("NotifyDataSetChanged")
    fun updateList() {
        if (b.list.isComputingLayout) return
        if (b.list.adapter == null) b.list.adapter = ListRad(c)
        else b.list.adapter?.notifyDataSetChanged()
        val isEmpty = b.list.adapter!!.itemCount == 0
        b.empty.isVisible = isEmpty
        b.list.isVisible = !isEmpty
    }

    override fun onRadarUpdated() {
        updateList()
    }

    override fun onPause() {
        super.onPause()
        c.m.radar.updateListeners.remove(this)
    }
}
