package ir.mahdiparastesh.homechat

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ir.mahdiparastesh.homechat.base.WiseService
import ir.mahdiparastesh.homechat.data.*
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.page.PageSet
import ir.mahdiparastesh.homechat.util.Notify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.toByteArray

class Receiver : WiseService() {
    private lateinit var server: ServerSocket
    private val ipToContactId = hashMapOf<String, Short>()
    private lateinit var nm: NotificationManagerCompat

    override fun onCreate() {
        super.onCreate()
        m.aliveReceiver = true
        CoroutineScope(Dispatchers.IO).launch {

            // notifications
            nm = NotificationManagerCompat.from(c)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannelGroup(Notify.ChannelGroup.CHAT.create(c))
                nm.createNotificationChannel(Notify.Channel.NEW_MESSAGE.create(c))
            }

            // load Contacts
            if (m.contacts == null) m.contacts = CopyOnWriteArrayList(dao.contacts())

            // initalise the server socket
            try {
                server = ServerSocket(sp.getInt(PageSet.PRF_PORT, -1))
                // close it only on user command and also make it null
                listen()
            } catch (_: BindException) {
                // "bind failed: EADDRINUSE (Address already in use)"
            } catch (_: SocketException) {
                // "listen failed: EADDRINUSE (Address already in use)"
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    @Suppress("RedundantSuspendModifier")
    private suspend fun listen() {
        server.accept() // listens until a connection is made (blocks the thread)
            .apply { CoroutineScope(Dispatchers.IO).launch { resolve(this@apply) } }
        listen()
    }

    @Throws(IOException::class, SocketException::class)
    private suspend fun resolve(socket: Socket) {
        val input = socket.getInputStream()
        val output = socket.getOutputStream() // don't use PrintWriter even with autoFlush

        // identify the Transmitter
        val fromIp = socket.remoteSocketAddress.toString().substring(1).split(":")[0]
        val dev = m.radar.devices.values.find { it.host.hostAddress == fromIp }
        val contact =
            if (dev != null) m.contacts?.find { it.equals(dev) }
            else m.contacts?.find { it.ip == fromIp }

        // act based on the Header
        val hb = input.read().toByte() // never put "output.read()" in a repeated function!!
        val header = Header.entries.find { it.value == hb }!!
        Log.println(Log.ASSERT, packageName, "RECEIVER: GOT ${header.name}")
        val len = header.getLength(input.readNBytesCompat(header.writeLengthInNBytes))
        val out: ByteArray = when (header) {

            Header.PAIR -> (if (dev != null) {
                // grab the list of contact IDs, suggest a number OUTSIDE them and create a Contact
                val contactsLen = input.readNBytesCompat(2).toNumber<Short>().toInt()
                val contactId = findUniqueId(
                    String(input.readNBytesCompat(contactsLen)),
                    dao.contactIds()
                )
                Contact(contactId, dev).apply {
                    dao.addContact(this)
                    m.contacts?.add(this)
                }

                ipToContactId[fromIp] = contactId

                // grab the list of chat IDs, suggest a number OUTSIDE them and create a Chat
                val chatsLen = input.readNBytesCompat(2).toNumber<Short>().toInt()
                val chatId = findUniqueId(
                    String(input.readNBytesCompat(chatsLen)),
                    dao.chatIds()
                )
                Chat(chatId, ipToContactId[fromIp].toString()).apply {
                    dao.addChat(this)
                    m.chats?.add(this)
                }
                m.radar.update(dao)

                contactId.toByteArray().plus(chatId.toByteArray())
            } else (-1).toShort().toByteArray())

            Header.MESSAGE -> if (contact != null) {
                val raw = input.readNBytesCompat(len).toList()
                Message(
                    id = raw.subList(0, 8).toNumber(),
                    chat = raw.subList(8, 10).toNumber(),
                    auth = contact.id,
                    time = raw.subList(10, 18).toNumber(),
                    repl = raw.subList(18, 26).toNumber<Long>().let { if (it == -1L) null else it },
                    type = raw.subList(26, 27)[0],
                    data = String(raw.subList(27, raw.size).toByteArray()),
                ).apply {
                    val theChat = m.chats?.find { it.id == chat } ?: dao.chat(chat)
                    dao.addMessage(this)

                    val seen = Seen(id, chat, contact.id, Chat.ME)
                    dao.addSeen(seen)
                    saw(seen)
                    theChat.checkForNewOnes(dao)

                    if (type == Message.Type.FILE.value) {
                        val dataSpl = data.split(Message.BINARY_SEP).toTypedArray()
                        for (bin in dao.binariesByMessage(id, chat, contact.id))
                            dataSpl[bin.pos_in_msg.toInt()] = bin.id.toString()
                        val newData = dataSpl.joinToString(Message.BINARY_SEP)
                        if (data != newData) {
                            data = newData
                            dao.updateMessage(this)
                        }
                    }

                    if (PageCht.handler != null)
                        PageCht.handler?.obtainMessage(PageCht.MSG_INSERTED, this)?.sendToTarget()
                    else {
                        if (!theChat.muted) notify(theChat, contact)
                        Main.handler?.obtainMessage(Main.MSG_NEW_MESSAGE)?.sendToTarget()
                    }
                }
                STAT_SUCCESS.toByteArray()
            } else STAT_CONTACT_NOT_FOUND.toByteArray()

            Header.SEEN -> if (contact != null) {
                val raw = input.readNBytesCompat(len).toList()
                dao.seen(
                    msg = raw.subList(0, 8).toNumber(),
                    chat = raw.subList(8, 10).toNumber(),
                    auth = Chat.ME,
                    contact = contact.id,
                )!!.apply {
                    seen_at = raw.subList(10, 18).toNumber()
                    dao.updateSeen(this)
                    PageCht.handler?.obtainMessage(PageCht.MSG_SEEN, this)?.sendToTarget()
                }
                STAT_SUCCESS.toByteArray()
            } else STAT_CONTACT_NOT_FOUND.toByteArray()

            Header.BINARY -> if (contact != null) {
                val raw = input.readNBytesCompat(len).toList()
                Binary(
                    sourceId = raw.subList(0, 8).toNumber(),
                    size = raw.subList(8, 16).toNumber(),
                    msg = raw.subList(16, 24).toNumber(),
                    chat = raw.subList(24, 26).toNumber(),
                    auth = contact.id,
                    createdAt = raw.subList(26, 34).toNumber(),
                    posInMsg = raw.subList(34, 35).toNumber(),
                    type =
                    if (raw.size == 35) null
                    else String(raw.subList(35, raw.size).toByteArray())
                ).apply {
                    val binId = dao.addBinary(this)
                    dao.message(msg, chat, auth)?.also { m ->
                        m.data = binId.toString()
                        dao.updateMessage(m)
                    }
                }
                STAT_SUCCESS.toByteArray()
            } else STAT_CONTACT_NOT_FOUND.toByteArray()

            else -> throw IllegalArgumentException("${header.name}: ${hb.toInt()}")
        }
        output.write(out)
        output.flush()
        socket.close() // necessary for the output stream to send
    }

    private fun findUniqueId(seq: String, ourIds: List<Short>): Short {
        val ids = if (seq.isNotEmpty()) seq.split(",").map { it.toShort() }
            .toMutableSet() else mutableSetOf()
        var chosenId: Short
        ourIds.forEach { ids.add(it) }
        onDestroy()
        do {
            chosenId = (0.toShort()..Short.MAX_VALUE).random().toShort()
        } while (chosenId in ids)
        return chosenId
    }

    private suspend fun Message.notify(theChat: Chat, sendingContact: Contact) {
        theChat.matchContacts(m.contacts!!)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) NotificationManagerCompat.from(c).notify(
            chat.toInt(),
            NotificationCompat.Builder(c, Notify.Channel.NEW_MESSAGE.id).apply {
                val style = NotificationCompat.MessagingStyle(sendingContact.person(theChat.pinned))
                for (unread in dao.theseMessage(dao.unseenInChat(chat), chat, sendingContact.id))
                    style.addMessage(
                        NotificationCompat.MessagingStyle.Message(
                            unread.data, unread.time,
                            theChat.contacts!!.find { it.id == unread.auth }?.person(theChat.pinned)
                        )
                    )
                setStyle(style)
                setCategory(Notification.CATEGORY_MESSAGE)
                setSmallIcon(R.mipmap.launcher_round)
                setContentIntent(
                    PendingIntent.getActivity(
                        c, 1, Intent(Main.Action.VIEW.s)
                            .setComponent(ComponentName(packageName, "$packageName.Main"))
                            .setData(android.net.Uri.parse(chat.toString())),
                        Notify.mutability(true)
                    )
                )
                setAutoCancel(true)
            }.build()
        )
    }

    override fun onDestroy() {
        m.aliveReceiver = false
        super.onDestroy()
    }

    enum class Header(
        val value: Byte, val writeLengthInNBytes: Int, val responseBytes: Int = Byte.SIZE_BYTES
    ) {
        PAIR(0x01, Short.SIZE_BYTES, 4),
        MESSAGE(0x02, Short.SIZE_BYTES),
        SEEN(0x03, Byte.SIZE_BYTES),
        BINARY(0x04, Byte.SIZE_BYTES),
        FILE(0x05, Int.SIZE_BYTES),
        PROF(0x0E, Int.SIZE_BYTES); // TODO Profile picture


        fun getLength(ba: ByteArray): Int {
            if (ba.size == Byte.SIZE_BYTES) return ba[0].toInt()
            val bb = ByteBuffer.wrap(ba)
            bb.rewind()
            return when (ba.size) {
                Short.SIZE_BYTES -> bb.short.toInt()
                Int.SIZE_BYTES -> bb.int
                Long.SIZE_BYTES -> bb.long.toInt()
                else -> 0
            }
        }

        fun putLength(size: Int): ByteArray {
            if (writeLengthInNBytes == 1) return byteArrayOf(size.toByte())
            val bb = ByteBuffer.allocate(writeLengthInNBytes)
            when (writeLengthInNBytes) {
                Short.SIZE_BYTES -> bb.putShort(size.toShort())
                Int.SIZE_BYTES -> bb.putInt(size)
                Long.SIZE_BYTES -> bb.putLong(size.toLong())
            }
            bb.rewind()
            return bb.array()
        }
    }

    companion object {
        const val STAT_SUCCESS = 0.toByte()
        const val STAT_CONTACT_NOT_FOUND = 1.toByte()
    }
}
