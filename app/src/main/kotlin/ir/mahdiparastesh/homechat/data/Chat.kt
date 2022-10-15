package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity
class Chat(
    var contactIds: String, // separated by CONTACT_SEP
    var name: String? = null,
    val dateInit: Long = Database.now(),
) : Radar.Item {
    @PrimaryKey(autoGenerate = true)
    var id: Short = 0

    @Ignore
    @Transient
    var contacts: List<Contact?>? = null

    fun size() = contacts?.size ?: contactIds.split(CONTACT_SEP).size

    fun isDirect() = size() == 1

    companion object {
        const val CONTACT_SEP = ","
    }
}
