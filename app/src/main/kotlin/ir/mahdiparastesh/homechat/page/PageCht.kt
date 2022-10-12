package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.databinding.PageChtBinding
import ir.mahdiparastesh.homechat.more.BasePage
import java.io.PrintWriter
import java.net.ConnectException
import java.net.Socket

class PageCht : BasePage<Main>() {
    private lateinit var b: PageChtBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageChtBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val address = arguments?.getString("device")?.split(":")
        if (address == null) {
            c.nav.navigateUp(); return; }

        Thread {
            try {
                Socket(address[0], address[1].toInt()).use {
                    val output = PrintWriter(it.getOutputStream())
                    output.write("KIROM")
                    output.flush()
                }
            } catch (e: ConnectException) {
                Main.handler?.obtainMessage(3, e.message.toString())?.sendToTarget()
            }
        }.start()
    }
}
