package ir.mahdiparastesh.homechat.util

import android.view.ContextThemeWrapper
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu

class EasyMenu(
    c: ContextThemeWrapper, v: View, res: Int, actions: HashMap<Int, (item: MenuItem) -> Unit>
) : PopupMenu(c, v) {
    init {
        setOnMenuItemClickListener {
            if (it.itemId in actions) {
                actions[it.itemId]!!(it)
                true
            } else false
        }
        inflate(res)
    }

    fun show(apply: EasyMenu.() -> Unit) {
        apply()
        show()
    }
}
