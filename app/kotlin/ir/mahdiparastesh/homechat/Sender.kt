package ir.mahdiparastesh.homechat

import android.content.Intent
import android.util.Log
import ir.mahdiparastesh.homechat.base.Persistent
import ir.mahdiparastesh.homechat.base.WiseService
import ir.mahdiparastesh.homechat.data.Binary
import ir.mahdiparastesh.homechat.data.Chat
import ir.mahdiparastesh.homechat.data.Message
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.util.Time
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader
import kotlin.collections.firstOrNull
import kotlin.text.split

class Sender : WiseService() {
    private var working = false

    override fun onCreate() {
        super.onCreate()
        m.aliveSender = true
    }

    /** The system first calls onCreate(), and then it calls onStartCommand(). */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!working) CoroutineScope(Dispatchers.IO).launch { start() }
        return START_NOT_STICKY
    }

    private suspend fun start() {
        working = true
        var address: Pair<String, Int>
        var available: Boolean

        for (target in m.queue.keys) {
            val contact = m.contacts!!.find { it.id == target }
            if (contact == null) {
                m.queue.remove(target)
                continue; }
            if (contact.ip == null || contact.port == null)
                continue

            address = Pair(contact.ip!!, contact.port!!)
            available = true
            for (o in m.queue[target]!!) if (available) Transmitter(address, o.header(), {
                when (o) {
                    is Message -> o.id.toByteArray()
                        .plus(o.chat.toByteArray())
                        .plus(o.time.toByteArray())
                        .plus((o.repl ?: -1L).toByteArray())
                        .plus(o.type.toByteArray())
                        .plus(o.data.encodeToByteArray())
                    is Seen -> o.msg.toByteArray()
                        .plus(o.chat.toByteArray())
                        .plus(o.seen_at!!.toByteArray())
                    is Binary -> o.id.toByteArray()
                        .plus(o.size.toByteArray())
                        .plus(o.msg.toByteArray())
                        .plus(o.chat.toByteArray())
                        .plus(o.created_at.toByteArray())
                        .plus(o.pos_in_msg.toByteArray())
                        .plus((o.type ?: "").encodeToByteArray())
                    else -> throw IllegalStateException()
                }
            }, { // on error:
                available = false
            }) { res -> // on success:
                val code = res.firstOrNull()
                Log.println(Log.ASSERT, packageName, "SENT " + o.header().name + " GOT " + code)
                if (code != Receiver.STAT_SUCCESS) {
                    when (code) {
                        Receiver.STAT_CONTACT_NOT_FOUND -> {
                            available = false
                            // TODO REPAIR
                        }
                    }
                    return@Transmitter
                }

                when (o) {
                    is Message -> dao.seen(o.id, o.chat, Chat.ME, contact.id)!!.apply {
                        sent_at = Time.now()
                        dao.updateSeen(this)
                        PageCht.handler?.obtainMessage(PageCht.MSG_SEEN, this)?.sendToTarget()
                    }
                    is Seen -> {
                        o.sent_at = Time.now()
                        dao.updateSeen(o)
                    }
                }
                m.dequeue(target, o)
            }
            else break
        }
        write(this)
        working = false
    }

    override fun onDestroy() {
        m.aliveSender = false
        super.onDestroy()
    }

    companion object {
        const val QUEUE_FILE = "queue.txt"
        const val Q_MESSAGE = "m"
        const val Q_SEEN = "s"
        const val Q_BINARY = "b"
        val charset = Charsets.US_ASCII


        fun init(c: Persistent, onIntent: (Intent.() -> Unit)? = null) {
            if (c.m.queue.isEmpty()) return
            val func: Intent.() -> Intent = { onIntent?.also { it() }; this }
            c.c.startService(Intent(c.c, Sender::class.java).func())
        }

        suspend fun read(c: Persistent) {
            if (!File(c.c.filesDir, QUEUE_FILE).exists()) return
            c.c.openFileInput(QUEUE_FILE).use {
                InputStreamReader(it, charset).readLines().forEach {
                    val spl = it.split("-")
                    val contact = c.m.contacts?.find { it.id == spl[0].toShort() } ?: return@forEach
                    c.m.enqueue(
                        contact.id, when (spl[1]) {
                            Q_MESSAGE ->
                                c.dao.message(spl[2].toLong(), spl[3].toShort(), Chat.ME)
                            Q_SEEN -> c.dao.seen(
                                spl[2].toLong(), spl[3].toShort(), spl[4].toShort(), Chat.ME
                            )
                            Q_BINARY ->
                                c.dao.binary(spl[2].toLong())
                            else -> throw IllegalStateException()
                        }!!
                    )
                }
            }
        }

        @Suppress("RedundantSuspendModifier")
        suspend fun write(c: Persistent) {
            c.c.openFileOutput(QUEUE_FILE, MODE_PRIVATE).use {
                val q = arrayListOf<String>()
                c.m.queue.forEach { (contact, items) ->
                    items.forEach { q.add(it.toQueue(c.m, contact)) }
                }
                it.write(q.joinToString("\n").toByteArray(charset))
            }
        }
    }

    interface Queuable { // "<receiver>-<type>-<indices**>"
        fun toQueue(m: Model, target: Short): String = when (this) {
            is Message -> "$target-$Q_MESSAGE-$id-$chat"
            is Seen -> "$target-$Q_SEEN-$msg-$chat-$auth"
            is Binary -> "$target-$Q_BINARY-$id"
            else -> throw IllegalArgumentException()
        }

        fun header(): Receiver.Header
    }
}
