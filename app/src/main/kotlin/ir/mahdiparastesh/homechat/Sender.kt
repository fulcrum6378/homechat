package ir.mahdiparastesh.homechat

import android.content.Context
import android.content.Intent
import ir.mahdiparastesh.homechat.Receiver.Companion.toByteArray
import ir.mahdiparastesh.homechat.data.Device.Companion.makeAddressPair
import ir.mahdiparastesh.homechat.data.Message
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.data.Seen
import ir.mahdiparastesh.homechat.more.WiseService
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

    // The system first calls onCreate(), and then it calls onStartCommand().
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent.extras?.containsKey(EXTRA_NEW_QUEUE) == true) {
            queue.add(intent.getStringExtra(EXTRA_NEW_QUEUE)!!)
            write()
        }
        if (!working) CoroutineScope(Dispatchers.IO).launch { start() }
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
                queue.removeAt(i); continue; }
            val o: Queuable? = when (spl[1]) {
                Q_MESSAGE -> dao.message(spl[2].toLong(), spl[3].toShort())
                Q_SEEN -> dao.seen(spl[2].toLong(), spl[3].toShort(), spl[4].toShort())
                else -> throw IllegalStateException()
            }
            if (o == null) {
                queue.removeAt(i); continue; }
            val target = m.radar.devices.find { it.equals(contact) }
            if (target == null) {
                i++; continue; }

            Transmitter(target.toString().makeAddressPair(), o.header(), {
                when (o) {
                    is Message -> o.id.toByteArray() // <id*4><chat*2><date*4><repl*4><data*n>
                        .plus(o.chat.toByteArray())
                        .plus(o.date.toByteArray())
                        .plus((o.repl ?: -1L).toByteArray())
                        .plus(o.data.encodeToByteArray())
                    is Seen -> o.msg.toByteArray() // <msg*4><chat*2><date*4>
                        .plus(o.chat.toByteArray())
                        .plus(o.date.toByteArray())
                    else -> throw IllegalStateException()
                }
            }) { res ->
                if (res?.first() == 0.toByte()) queue.removeAt(i)
            }
            write()
        }
        working = false
    }

    private fun read() {
        if (File(c.filesDir, QUEUE_FILE).exists()) c.openFileInput(QUEUE_FILE).use {
            queue = ArrayList(InputStreamReader(it, charset).readLines())
        } else queue = arrayListOf()
    }

    private fun write() {
        c.openFileOutput(QUEUE_FILE, Context.MODE_PRIVATE).use {
            it.write(queue.joinToString("\n").toByteArray(charset))
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


        fun init(c: Context, onIntent: Intent.() -> Intent = { this }) {
            c.startService(Intent(c, Sender::class.java).onIntent())
        }
    }

    interface Queuable { // "<receiver>-<type>-<indices**>"
        fun toQueue(m: Model): List<String> = when (this) {
            is Message -> m.chats!!.find { it.id == chat }!!.contacts!!
                .map { "${it!!.id}-$Q_MESSAGE-$id-$chat" }
            is Seen -> m.chats!!.find { it.id == chat }!!.contacts!!
                .map { "${it!!.id}-$Q_SEEN-$msg-$chat-$contact" }
            else -> throw IllegalArgumentException()
        }

        fun header(): Receiver.Header = when (this) {
            is Message -> Receiver.Header.values().find { it.value == type }!!
            is Seen -> Receiver.Header.SEEN
            else -> throw IllegalArgumentException()
        }
    }
}
