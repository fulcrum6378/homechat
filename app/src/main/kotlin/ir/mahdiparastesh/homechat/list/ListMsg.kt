package ir.mahdiparastesh.homechat.list

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.databinding.ListMsgBinding
import ir.mahdiparastesh.homechat.more.AnyViewHolder

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
            strokeWidth = .5f
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListMsgBinding> =
        AnyViewHolder(ListMsgBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListMsgBinding>, i: Int) {
        val msg = c.m.messages?.getOrNull(i) ?: return

        // Date
        h.b.date.text = c.dateFormat.format(msg.date)

        // Me or Them?
        h.b.area.layoutParams = (h.b.area.layoutParams as ConstraintLayout.LayoutParams).apply {
            horizontalBias = if (msg.me()) 1f else 0f
        }
        h.b.body.layoutParams = (h.b.body.layoutParams as ConstraintLayout.LayoutParams).apply {
            horizontalBias = if (msg.me()) 1f else 0f
        }
        h.b.body.background = if (msg.me()) meStyle else themStyle

        // Data
        h.b.text.text = msg.data

        h.b.time.text = c.timeFormat.format(msg.date)
    }

    override fun getItemCount(): Int = c.m.messages?.size ?: 0
}
