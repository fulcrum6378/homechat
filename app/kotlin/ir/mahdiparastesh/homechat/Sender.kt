package ir.mahdiparastesh.homechat

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Message
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.more.WiseService
import ir.mahdiparastesh.homechat.page.PageCht
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStreamReader

class Sender : WiseService() {
    private lateinit var queue: ArrayList<String>
    private var working = false
    private var i = 0

    override fun onCreate() {
        super.onCreate()
        m.aliveSender = true
    }

    /** The system first calls onCreate(), and then it calls onStartCommand(). */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!working || intent.extras?.containsKey(EXTRA_NEW_QUEUE) == true
        ) CoroutineScope(Dispatchers.IO).launch {
            if (intent.extras?.containsKey(EXTRA_NEW_QUEUE) == true) {
                read()
                queue.addAll(intent.getStringArrayExtra(EXTRA_NEW_QUEUE)!!)
                write()
            }
            if (!working) start()
        }
        return START_NOT_STICKY
    }

    private suspend fun start() {
        working = true
        read()
        i = 0
        while (i < queue.size) {
            val spl = queue[i].split("-")
            val contact = m.contacts?.find { it.id == spl[0].toShort() }
            if (contact == null) {
                queue.safeRemoveAt(i); continue; }
            val o: Queuable? = when (spl[1]) {
                Q_MESSAGE -> dao.message(spl[2].toLong(), spl[3].toShort())
                Q_SEEN -> dao.seen(spl[2].toLong(), spl[3].toShort(), contact.id)
                else -> throw IllegalStateException()
            }
            if (o == null) {
                queue.safeRemoveAt(i); continue; }
            if (contact.ip == null || contact.port == null) {
                val dev = m.radar.devices.find { it.equals(contact) }
                contact.ip = dev?.host?.hostAddress
                contact.port = dev?.port
            }
            if (contact.ip == null) {
                i++; continue; }

            Transmitter(Pair(contact.ip!!, contact.port!!), o.header(), {
                when (o) {
                    is Message -> o.id.toByteArray()
                        .plus(o.chat.toByteArray())
                        .plus(o.date.toByteArray())
                        .plus((o.repl ?: -1L).toByteArray())
                        .plus(o.data.encodeToByteArray())
                    is Seen -> o.msg.toByteArray()
                        .plus(o.chat.toByteArray())
                        .plus(o.dateSeen!!.toByteArray())
                    else -> throw IllegalStateException()
                }
            }) { res ->
                if (res?.firstOrNull() == 0.toByte()) {
                    queue.safeRemoveAt(i)
                    if (o is Message) dao.seen(o.id, o.chat, contact.id)!!.apply {
                        dateSent = Database.now()
                        dao.updateSeen(this)
                        PageCht.handler?.obtainMessage(PageCht.MSG_SEEN, o.chat.toInt(), 0, this)
                            ?.sendToTarget()
                    }
                } else i++
            }
            write()
        }
        working = false
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun read() {
        if (::queue.isInitialized) return
        if (File(c.filesDir, QUEUE_FILE).exists()) c.openFileInput(QUEUE_FILE).use {
            queue = ArrayList(InputStreamReader(it, charset).readLines())
        } else queue = arrayListOf()
    }

    @Suppress("RedundantSuspendModifier")
    private suspend fun write() {
        c.openFileOutput(QUEUE_FILE, Context.MODE_PRIVATE).use {
            it.write(queue.joinToString("\n").toByteArray(charset))
        }
    }

    private fun ArrayList<String>.safeRemoveAt(index: Int) {
        try {
            removeAt(index)
        } catch (_: IndexOutOfBoundsException) {
        }
    }

    override fun onDestroy() {
        m.aliveSender = false
        super.onDestroy()
    }

    companion object {
        const val QUEUE_FILE = "queue.txt"
        const val Q_MESSAGE = "m"
        const val Q_SEEN = "s"
        const val EXTRA_NEW_QUEUE = "new_queue"
        val charset = Charsets.US_ASCII


        fun init(c: ContextWrapper, onIntent: (Intent.() -> Unit)? = null) {
            val func: Intent.() -> Intent = { onIntent?.also { it() }; this }
            c.startService(Intent(c, Sender::class.java).func())
        }
    }

    interface Queuable { // "<receiver>-<type>-<indices**>"
        fun toQueue(m: Model): Array<String> = when (this) {
            is Message -> m.chats!!.find { it.id == chat }!!.contacts!!
                .map { "${it.id}-$Q_MESSAGE-$id-$chat" }.toTypedArray()
            is Seen -> arrayOf("$contact-$Q_SEEN-$msg-$chat")
            // m.chats!!.find { it.id == chat }!!.contacts!!.map { }
            else -> throw IllegalArgumentException()
        }

        fun header(): Receiver.Header = when (this) {
            is Message -> Receiver.Header.values().find { it.value == type }!!
            is Seen -> Receiver.Header.SEEN
            else -> throw IllegalArgumentException()
        }
    }
}
