package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.Sender
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Database.Companion.calendar
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.databinding.ListMsgBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ListMsg(private val c: Main/*, private val f: PageCht*/) :
    RecyclerView.Adapter<AnyViewHolder<ListMsgBinding>>() {

    private val rtl = c.resources.getBoolean(R.bool.dirRtl)
    private val cornerFamily = CornerFamily.ROUNDED
    private val cornerSize = c.resources.getDimension(R.dimen.mediumCornerSize)
    private val meStyle: MaterialShapeDrawable by lazy {
        MaterialShapeDrawable(
            ShapeAppearanceModel.Builder().apply {
                setTopLeftCorner(cornerFamily, cornerSize)
                setTopRightCorner(cornerFamily, cornerSize)
                if (rtl) setBottomRightCorner(cornerFamily, cornerSize)
                else setBottomLeftCorner(cornerFamily, cornerSize)
            }.build()
        ).apply { fillColor = ContextCompat.getColorStateList(c, R.color.msg_me) }
    }
    private val themStyle: MaterialShapeDrawable by lazy {
        MaterialShapeDrawable(
            ShapeAppearanceModel.Builder().apply {
                setTopLeftCorner(cornerFamily, cornerSize)
                setTopRightCorner(cornerFamily, cornerSize)
                if (rtl) setBottomLeftCorner(cornerFamily, cornerSize)
                else setBottomRightCorner(cornerFamily, cornerSize)
            }.build()
        ).apply {
            fillColor = ContextCompat.getColorStateList(c, R.color.msg_them)
            strokeColor = ContextCompat.getColorStateList(c, R.color.msg_them_stroke)
            strokeWidth = 1f
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListMsgBinding> =
        AnyViewHolder(ListMsgBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListMsgBinding>, i: Int) {
        val msg = c.m.messages?.getOrNull(i) ?: return

        // Date
        val cal = msg.date.calendar()
        var showDate = true
        if (i > 0) {
            val prev = c.m.messages!![i - 1].date.calendar()
            if (cal[Calendar.YEAR] == prev[Calendar.YEAR] &&
                cal[Calendar.MONTH] == prev[Calendar.MONTH] &&
                cal[Calendar.DAY_OF_MONTH] == prev[Calendar.DAY_OF_MONTH]
            ) showDate = false
        }
        h.b.date.isVisible = showDate
        if (showDate) h.b.date.text = c.dateFormat.format(msg.date)

        // Layout
        h.b.area.layoutParams = (h.b.area.layoutParams as ConstraintLayout.LayoutParams)
            .apply { horizontalBias = if (msg.me()) 1f else 0f }
        h.b.body.layoutParams = (h.b.body.layoutParams as ConstraintLayout.LayoutParams)
            .apply { horizontalBias = if (msg.me()) 1f else 0f }
        h.b.body.background = if (msg.me()) meStyle else themStyle

        // Data
        h.b.text.text = msg.data

        // Time
        h.b.time.text = c.timeFormat.format(msg.date)

        // Seen if not
        var notSeen: List<Seen>? = null
        if (!msg.me()
            && msg.status!!
                .filter { it.dateSeen == null }
                .apply { notSeen = this }
                .isNotEmpty()
        ) CoroutineScope(Dispatchers.IO).launch {
            var queue = arrayOf<String>()
            notSeen!!.forEach {
                it.dateSeen = Database.now()
                c.dao.addSeen(it)
                queue = queue.plus(it.toQueue(c.m))
            }
            Sender.init(c) { putExtra(Sender.EXTRA_NEW_QUEUE, queue) }
        }
    } // Don't put onBindView in a companion object for exporting, make a dynamic class

    override fun getItemCount(): Int = c.m.messages?.size ?: 0
}
