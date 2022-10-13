package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Chat(
    var contacts: String, // separated by ","
) {
    @PrimaryKey(autoGenerate = true)
    var id: Short = 0
}
