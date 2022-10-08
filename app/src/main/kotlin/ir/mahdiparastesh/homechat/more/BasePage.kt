package ir.mahdiparastesh.homechat.more

import androidx.fragment.app.Fragment

abstract class BasePage<C> : Fragment() where C : BaseActivity {
    @Suppress("UNCHECKED_CAST")
    protected val c: C by lazy { activity as C }
}
