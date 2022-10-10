package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Contact(
    @PrimaryKey(autoGenerate = true) var id: Long,
    var name: String,
    var lastIp: String,
    var email: String?,
    var phone: String?,
    var lastActiveDate: Long,
)
