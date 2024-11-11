@file:Suppress("unused")

package ir.mahdiparastesh.homechat.util

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SafeLinearLayoutManager : LinearLayoutManager {
    constructor(
        context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(
        context: Context, @RecyclerView.Orientation orientation: Int, reverseLayout: Boolean
    ) : super(context, orientation, reverseLayout)

    constructor(context: Context) : super(context)

    override fun supportsPredictiveItemAnimations(): Boolean = false
}
