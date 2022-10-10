package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Contact(
    var name: String,
    var lastIp: String,
    var email: String?,
    var phone: String?,
    var lastActivity: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
}
