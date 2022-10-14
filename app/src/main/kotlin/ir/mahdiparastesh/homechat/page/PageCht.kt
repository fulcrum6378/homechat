package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.Radio
import ir.mahdiparastesh.homechat.Transmitter
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.databinding.PageChtBinding
import ir.mahdiparastesh.homechat.more.BasePage
import java.nio.ByteBuffer

class PageCht : BasePage<Main>() {
    private lateinit var b: PageChtBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val address = arguments?.getString(ARG_DEVICE)?.makeAddressPair()
        val dev = c.m.radar.value?.find { it.host.hostAddress == address?.first }
        if (address == null || dev == null) {
            c.nav.navigateUp(); return; }

        if (dev.contact == null) pair(address, dev)
    }

    private fun pair(address: Pair<String, Int>, dev: Device) {
        Transmitter(address, Radio.Header.PAIR, Short.SIZE_BYTES, {
            c.dao.contactIds().joinToString(",").encodeToByteArray()
        }) { res ->
            if (res == null) {
                return@Transmitter; }
            c.dao.addContact(
                Contact(
                    ByteBuffer.wrap(res).short, dev.name, address.first, Database.now(),
                    dev.email, dev.phone, Database.now()
                )
            )
        }
    }

    companion object {
        const val ARG_DEVICE = "device"
    }
}
