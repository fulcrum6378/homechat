package ir.mahdiparastesh.homechat

import android.content.Context
import android.content.Intent
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
                wrong(); continue; }
            val obj: Queuable? = when (spl[1]) {
                Q_MESSAGE -> dao.message(spl[2].toLong(), spl[3].toShort())
                Q_SEEN -> dao.seen(spl[2].toLong(), spl[3].toShort(), spl[4].toShort())
                else -> throw IllegalStateException()
            }
            if (obj == null) {
                wrong(); continue; }
            val target = m.radar.devices.find { it.equals(contact) }
            if (target == null) {
                i++; continue; }

            Transmitter(target.toString().makeAddressPair(), obj.header(), {
                when (obj) {
                    is Message -> {}
                    is Seen -> {}
                    else -> throw IllegalStateException()
                }
            }) { res ->

            }
            write()
        }
        working = false
    }

    private fun wrong() {
        queue.removeAt(i)
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
        val charset = Charsets.US_ASCII
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
