package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Chat(
    override var name: String,
    var contacts: String, // separated by ","
    val dateInit: Long,
) : Radar.Item {
    @PrimaryKey(autoGenerate = true)
    var id: Short = 0

    fun size() = contacts.split(",").size
}
