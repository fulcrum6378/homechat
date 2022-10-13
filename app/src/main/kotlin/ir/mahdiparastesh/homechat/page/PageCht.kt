package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.Radio
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.databinding.PageChtBinding
import ir.mahdiparastesh.homechat.more.BasePage

class PageCht : BasePage<Main>() {
    private lateinit var b: PageChtBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val address = arguments?.getString(ARG_DEVICE)?.makeAddressPair()
        if (address == null) {
            c.nav.navigateUp(); return; }

        Transmitter(address, Radio.Header.PAIR) {
            c.db.dao().contactIds().joinToString(",").encodeToByteArray()
        }
    }

    companion object {
        const val ARG_DEVICE = "device"
    }
}
