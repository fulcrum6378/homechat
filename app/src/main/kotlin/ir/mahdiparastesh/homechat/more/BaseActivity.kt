package ir.mahdiparastesh.homechat.more

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.R

abstract class BaseActivity : AppCompatActivity() {
    val c: Context get() = applicationContext
    lateinit var m: Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
    }

    override fun setContentView(root: View?) {
        super.setContentView(root)
        root?.layoutDirection =
            if (!resources.getBoolean(R.bool.dirRtl)) ViewGroup.LAYOUT_DIRECTION_LTR
            else ViewGroup.LAYOUT_DIRECTION_RTL
    }
}
