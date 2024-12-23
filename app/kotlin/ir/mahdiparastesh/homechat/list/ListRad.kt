package ir.mahdiparastesh.homechat.list

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.base.AnyViewHolder
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.databinding.ListRadBinding
import ir.mahdiparastesh.homechat.databinding.PairingBinding
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.util.EasyMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListRad(private val c: Main) : RecyclerView.Adapter<AnyViewHolder<ListRadBinding>>() {
    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): AnyViewHolder<ListRadBinding> =
        AnyViewHolder(ListRadBinding.inflate(c.layoutInflater, parent, false))

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(h: AnyViewHolder<ListRadBinding>, i: Int) {
        val item = c.m.radar.getOrNull(i) ?: return
        val chat = item as? Chat
        val dev = item as? Device

        // texts
        h.b.title.text = "${i + 1}. " + (dev?.name ?: chat!!.title())
        h.b.subtitle.text =
            dev?.let { it.host.hostAddress ?: "Unknown IP" } ?: chat!!.onlineStatus(c)

        // counter badge
        val hasNew = chat?.newOnes != null && chat.newOnes!! > 0
        if (hasNew) h.b.badge.text = chat.newOnes.toString()
        h.b.badge.isVisible = hasNew

        // icons
        h.b.mute.isVisible = chat?.muted == true
        h.b.pin.isVisible = chat?.pinned == true

        // clicks
        if (chat != null) {
            h.b.root.setOnClickListener {
                c.nav.navigate(
                    R.id.action_page_rad_to_page_cht, bundleOf(PageCht.ARG_CHAT_ID to chat.id)
                )
            }
            h.b.root.setOnLongClickListener {
                EasyMenu(
                    c, it, R.menu.list_rad_chat, hashMapOf(
                        R.id.chat_pin to { chat.pin(true) },
                        R.id.chat_unpin to { chat.pin(false) },
                        R.id.chat_mute to { chat.mute(true) },
                        R.id.chat_unmute to { chat.mute(false) },
                    )
                ).show {
                    if (chat.pinned) {
                        menu.findItem(R.id.chat_pin).isVisible = false
                        menu.findItem(R.id.chat_unpin).isVisible = true
                    }
                    if (chat.muted) {
                        menu.findItem(R.id.chat_mute).isVisible = false
                        menu.findItem(R.id.chat_unmute).isVisible = true
                    }
                }
                true
            }
        } else if (dev != null) {
            h.b.root.setOnClickListener { dev.pairDialog() }
            h.b.root.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = c.m.radar.size

    @SuppressLint("InflateParams")
    private fun Device.pairDialog() {
        val devInfo = StringBuilder()
            .append(c.getString(R.string.devName)).append(": ")
            .append(name).append("\n")
            .append(c.getString(R.string.unique)).append(": ")
            .append(unique ?: c.getString(R.string.notSet)).append("\n")
            .append(c.getString(R.string.ipAddress)).append(": ")
            .append(host.hostAddress ?: c.getString(R.string.unknown)).append("\n")
            .toString()
        val bp = PairingBinding.inflate(c.layoutInflater)

        val dialog = MaterialAlertDialogBuilder(c).apply {
            setTitle(R.string.newContact)
            setMessage(devInfo)
            setView(bp.root)
            setPositiveButton(R.string.pair, null)
            setNegativeButton(R.string.cancel, null)
            setCancelable(true)
        }.show()

        val posButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val negButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        posButton.setOnClickListener {
            dialog.setMessage(devInfo)
            dialog.setCancelable(false)
            bp.lottie.resumeAnimation()
            posButton.isVisible = false
            negButton.isVisible = false

            CoroutineScope(Dispatchers.IO).launch {
                pair(c, { msg ->
                    bp.lottie.pauseAnimation()
                    dialog.setMessage(devInfo + "\n" + c.getString(msg) + "\n")
                    dialog.setCancelable(true)
                    posButton.isVisible = true
                    negButton.isVisible = true
                }) { dialog.dismiss() }
            }
        }
    }

    private fun Chat.pin(bb: Boolean) {
        pinned = bb
        CoroutineScope(Dispatchers.IO).launch {
            c.dao.updateChat(this@pin)
            val oldIndex = c.m.radar.indexOf(this@pin)
            c.m.radar.sort()
            val newIndex = c.m.radar.indexOf(this@pin)
            if (newIndex != -1) withContext(Dispatchers.Main) {
                if (oldIndex != newIndex) notifyItemMoved(oldIndex, newIndex)
                notifyItemChanged(oldIndex)
                notifyItemChanged(newIndex)
            }
        }
    }

    private fun Chat.mute(bb: Boolean) {
        muted = bb
        CoroutineScope(Dispatchers.IO).launch {
            c.dao.updateChat(this@mute)
            val index = c.m.radar.indexOf(this@mute)
            if (index != -1) withContext(Dispatchers.Main) {
                notifyItemChanged(index)
            }
        }
    }
}
