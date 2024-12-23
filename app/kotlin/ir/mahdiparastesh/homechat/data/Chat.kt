package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ir.mahdiparastesh.homechat.R
import ir.mahdiparastesh.homechat.base.Persistent
import ir.mahdiparastesh.homechat.util.Time
import ir.mahdiparastesh.homechat.util.Time.calendar

@Suppress("PropertyName")
@Entity(
    tableName = "chat",
    indices = [Index("id")],
)
class Chat(
    @PrimaryKey var id: Short = 0,
    /** Items are separated by [CONTACT_SEP]. */
    var contact_ids: String,
    var name: String? = null,
    var pinned: Boolean = false,
    var muted: Boolean = false,
    val created_at: Long = Time.now(),
) : Radar.Item {

    @Ignore
    @Transient
    var contacts: List<Contact>? = null

    @Ignore
    @Transient
    var newOnes: Int? = null

    fun isDirect() = (contacts?.size ?: contact_ids.split(CONTACT_SEP).size) == 1

    fun matchContacts(allContacts: List<Contact>) {
        contacts = contact_ids.split(CONTACT_SEP)
            .map { id -> allContacts.find { it.id == id.toShort() }!! }
    }

    fun title(): String = name ?: contacts?.firstOrNull()?.name() ?: ""

    fun onlineStatus(c: Persistent): String = if (isDirect()) {
        val contact = contacts?.firstOrNull()
        if (contact?.ip?.let { ip -> ip in c.m.radar.onlineIPs } == true) c.c.getString(R.string.online)
        else {
            val lo = contact?.online_at
            if (lo == null) c.c.getString(R.string.offline)
            else c.c.getString(R.string.lastOnline) +
                    Time.distance(c.c, lo.calendar(), append = R.string.ago)
        }
    } else {
        if (!contacts.isNullOrEmpty()) c.c.getString(
            R.string.peopleOnline,
            contacts!!.count { it.ip?.let { ip -> ip in c.m.radar.onlineIPs } == true },
            contacts!!.size
        )
        else c.c.getString(R.string.empty)
    }

    suspend fun checkForNewOnes(dao: Database.DAO) {
        newOnes = dao.countUnseenInChat(id)
    }

    companion object {
        const val CONTACT_SEP = ","
        const val ME = (-1).toShort()
    }
}
