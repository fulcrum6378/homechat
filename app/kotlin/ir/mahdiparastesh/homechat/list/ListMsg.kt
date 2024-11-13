package ir.mahdiparastesh.homechat.list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.RippleDrawable
import android.view.View
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
import ir.mahdiparastesh.homechat.base.AnyViewHolder
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.databinding.ListMsgBinding
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.util.EasyMenu
import ir.mahdiparastesh.homechat.util.Time
import ir.mahdiparastesh.homechat.util.Time.calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class ListMsg(private val c: Main, private val f: PageCht) :
    RecyclerView.Adapter<AnyViewHolder<ListMsgBinding>>() {

    private val rtl = c.resources.getBoolean(R.bool.dirRtl)
    private val cornerFamily = CornerFamily.ROUNDED
    private val cornerSize = c.resources.getDimension(R.dimen.mediumCornerSize)

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListMsgBinding> =
        AnyViewHolder(ListMsgBinding.inflate(c.layoutInflater, parent, false))

    override fun onBindViewHolder(h: AnyViewHolder<ListMsgBinding>, i: Int) {
        val msg = c.m.messages?.getOrNull(i) ?: return
        val isMe = msg.me()
        val hasReply = msg.repl != null

        // date
        val cal = msg.time.calendar()
        var showDate = true
        if (i > 0) {
            val prev = c.m.messages!![i - 1].time.calendar()
            if (cal[Calendar.YEAR] == prev[Calendar.YEAR] &&
                cal[Calendar.MONTH] == prev[Calendar.MONTH] &&
                cal[Calendar.DAY_OF_MONTH] == prev[Calendar.DAY_OF_MONTH]
            ) showDate = false
        }
        h.b.date.isVisible = showDate
        if (showDate) h.b.date.text = Time.formatDate(cal)

        // layout
        h.b.area.layoutParams = (h.b.area.layoutParams as ConstraintLayout.LayoutParams)
            .apply { horizontalBias = if (isMe) 1f else 0f }
        h.b.body.layoutParams = (h.b.body.layoutParams as ConstraintLayout.LayoutParams).apply {
            horizontalBias = if (isMe) 1f else 0f
            topToTop =
                if (!hasReply) ConstraintLayout.LayoutParams.PARENT_ID
                else R.id.barrier
        }
        h.b.body.background = MaterialShapeDrawable(
            ShapeAppearanceModel.Builder().apply {
                setTopLeftCorner(cornerFamily, cornerSize)
                setTopRightCorner(cornerFamily, cornerSize)
                if ((isMe && rtl) || (!isMe && !rtl))
                    setBottomRightCorner(cornerFamily, cornerSize)
                if ((isMe && !rtl) || (!isMe && rtl))
                    setBottomLeftCorner(cornerFamily, cornerSize)
            }.build()
        ).let {
            it.fillColor =
                ContextCompat.getColorStateList(c, if (isMe) R.color.msg_me else R.color.msg_them)
            if (!isMe) {
                it.strokeColor = ContextCompat.getColorStateList(c, R.color.msg_them_stroke)
                it.strokeWidth = 1f
            }
            // c.themeColor(com.google.android.material.R.attr.rippleColor)
            RippleDrawable(ContextCompat.getColorStateList(c, R.color.msg_ripple)!!, it, null)
        }

        // reply
        var repliedToIdx = i
        val repliedTo = msg.repl?.let { repl ->
            repliedToIdx = c.m.messages!!.indexOfFirst { it.id == repl }
            c.m.messages!!.getOrNull(repliedToIdx)
        }
        h.b.replyText.text = repliedTo?.data
        h.b.reply.isVisible = hasReply
        h.b.reply.setOnClickListener(if (!hasReply) null else object : View.OnClickListener {
            override fun onClick(v: View?) {
                f.b.list.scrollToPosition(repliedToIdx)
            }
        })

        // data
        h.b.text.text = msg.data

        // time
        h.b.time.text = Time.formatTime(msg.time)

        // seen status of mine
        h.b.seen.isVisible = isMe
        if (isMe) h.b.seen.setImageResource(when {
            msg.status?.any { it.dateSeen != null } == true -> R.drawable.seen
            msg.status?.any { it.dateSent != null } == true -> R.drawable.sent
            else -> R.drawable.no_signal
        })

        // seen theirs if not
        var notSeen: List<Seen>? = null
        if (!isMe &&
            msg.status!!.filter { it.dateSeen == null }.apply { notSeen = this }.isNotEmpty()
        ) CoroutineScope(Dispatchers.IO).launch {
            var queue = arrayOf<String>()
            notSeen!!.forEach {
                it.dateSeen = Time.now()
                c.dao.updateSeen(it)
                msg.status!!.add(it) // TODO won't you replace them?!?
                queue = queue.plus(it.toQueue(c.m, msg.auth))
            }
            f.chat.checkForNewOnes(c.dao)
            Sender.init(c) { putExtra(Sender.EXTRA_NEW_QUEUE, queue) }
        }

        // clicks
        h.b.body.setOnClickListener {
            EasyMenu(
                c, it, R.menu.list_msg, hashMapOf(
                    R.id.msg_reply to {
                        f.reply(msg)
                    },
                    R.id.msg_copy to {
                        (c.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                            ?.setPrimaryClip(ClipData.newPlainText(msg.data, msg.data))
                    },
                )
            ).show()
        }
    } // Don't put onBindView in a companion object for exporting, make a dynamic class

    override fun getItemCount(): Int = c.m.messages?.size ?: 0
}
