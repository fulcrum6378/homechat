package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.databinding.PageThdBinding
import ir.mahdiparastesh.homechat.more.BasePage

class PageThd : BasePage<Main>() {
    private lateinit var b: PageThdBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = PageThdBinding.inflate(inflater, container, false).apply { b = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Toast.makeText(c, arguments?.getString("device").toString(), Toast.LENGTH_LONG).show()
    }
}
