package ir.mahdiparastesh.homechat

import android.os.Bundle
import ir.mahdiparastesh.homechat.databinding.MainBinding
import ir.mahdiparastesh.homechat.more.BaseActivity

class Main : BaseActivity() {
    private val b: MainBinding by lazy { MainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)
    }
}
