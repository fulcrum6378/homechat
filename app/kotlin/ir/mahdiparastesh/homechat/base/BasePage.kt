package ir.mahdiparastesh.homechat.base

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import ir.mahdiparastesh.homechat.Main

abstract class BasePage<C> : Fragment() where C : AppCompatActivity {
    @Suppress("UNCHECKED_CAST")
    protected val c: C by lazy { activity as C }
    protected var onDestChanged: NavController.OnDestinationChangedListener? = null

    abstract fun rv(): RecyclerView?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (c is Main) {
            onDestChanged?.also { (c as Main).nav.addOnDestinationChangedListener(it) }

            rv()?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    onListScrolled()
                }
            })
        }
    }

    abstract fun tbTitle(): String

    override fun onResume() {
        super.onResume()
        updateShadow()

        if (c is Main) (c as Main).also { c ->
            c.b.toolbar.title = tbTitle()
            c.tbSubtitleListener.onRadarUpdated()
        }
    }

    open fun onListScrolled() {
        updateShadow()
    }

    private fun updateShadow() {
        rv()?.apply {
            if (c is Main) (c as Main).b.tbShadow.isInvisible = computeVerticalScrollOffset() == 0
        }
    }

    override fun onDestroy() {
        if (c is Main) onDestChanged?.also { (c as Main).nav.removeOnDestinationChangedListener(it) }
        super.onDestroy()
    }
}
