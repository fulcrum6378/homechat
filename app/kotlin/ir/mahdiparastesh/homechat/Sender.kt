package ir.mahdiparastesh.homechat

import android.content.ContextWrapper
import android.content.Intent
import ir.mahdiparastesh.homechat.base.Persistent
import ir.mahdiparastesh.homechat.base.WiseService
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
import kotlin.text.split

class Sender : WiseService() {
    private var working = false
    private var i = 0

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
        m.pendingContacts.clear()
        i = 0
        for (target in m.queue.keys) {
            val contact = m.contacts?.find { it.id == target }
            if (contact == null) {
                m.queue.remove(target)
                continue; }
            if (contact.ip == null || contact.port == null) {
                m.pendingContacts.add(contact.id)
                continue; }

            val addr = Pair(contact.ip!!, contact.port!!)
            for (o in m.queue[target]!!) if (!Transmitter(addr, o.header(), {
                    when (o) {
                        is Message -> o.id.toByteArray()
                            .plus(o.chat.toByteArray())
                            .plus(o.time.toByteArray())
                            .plus((o.repl ?: -1L).toByteArray())
                            .plus(o.data.encodeToByteArray())
                        is Seen -> o.msg.toByteArray()
                            .plus(o.chat.toByteArray())
                            .plus(o.dateSeen!!.toByteArray())
                        else -> throw IllegalStateException()
                    }
                }) { res ->
                    when (o) {
                        is Message -> dao.seen(o.id, o.chat, contact.id)!!.apply {
                            dateSent = Time.now()
                            dao.updateSeen(this)
                            PageCht.handler?.obtainMessage(
                                PageCht.MSG_SEEN, o.chat.toInt(), 0, this
                            )?.sendToTarget()
                        }
                        is Seen -> {
                            o.dateSent = Time.now()
                            dao.updateSeen(o)
                        }
                    }
                    m.dequeue(target, o)

                }) {;m.pendingContacts.add(contact.id); break; }
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
        val charset = Charsets.US_ASCII


        fun init(c: ContextWrapper, onIntent: (Intent.() -> Unit)? = null) {
            val func: Intent.() -> Intent = { onIntent?.also { it() }; this }
            c.startService(Intent(c, Sender::class.java).func())
        }

        suspend fun read(c: Persistent) {
            if (!File(c.c.filesDir, QUEUE_FILE).exists()) return
            c.c.openFileInput(QUEUE_FILE).use {
                InputStreamReader(it, charset).readLines().forEach {
                    val spl = it.split("-")
                    val contact = c.m.contacts?.find { it.id == spl[0].toShort() } ?: return@forEach
                    c.m.enqueue(
                        contact.id, when (spl[1]) {
                            Q_MESSAGE -> c.dao.message(spl[2].toLong(), spl[3].toShort(), Chat.ME)
                            Q_SEEN -> c.dao.seen(spl[2].toLong(), spl[3].toShort(), Chat.ME)
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
            is Seen -> "$target-$Q_SEEN-$msg-$chat"
            else -> throw IllegalArgumentException()
        }

        fun header(): Receiver.Header = when (this) {
            is Message -> Receiver.Header.entries.find { it.value == type }!!
            is Seen -> Receiver.Header.SEEN
            else -> throw IllegalArgumentException()
        }

        fun contact(): Short = when (this) {
            is Message -> auth
            is Seen -> contact
            else -> throw IllegalArgumentException()
        }
    }
}
