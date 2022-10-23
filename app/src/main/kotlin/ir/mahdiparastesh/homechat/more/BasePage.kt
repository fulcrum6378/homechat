package ir.mahdiparastesh.homechat.more

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import ir.mahdiparastesh.homechat.Main

abstract class BasePage<C> : Fragment() where C : AppCompatActivity {
    @Suppress("UNCHECKED_CAST")
    protected val c: C by lazy { activity as C }
    protected var onDestChanged: NavController.OnDestinationChangedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (c is Main) onDestChanged?.also { (c as Main).nav.addOnDestinationChangedListener(it) }
    }

    override fun onDestroy() {
        if (c is Main) onDestChanged?.also { (c as Main).nav.removeOnDestinationChangedListener(it) }
        super.onDestroy()
    }
}
