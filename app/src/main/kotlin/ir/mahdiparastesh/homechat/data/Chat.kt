package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Chat(
    @PrimaryKey(autoGenerate = true) var id: Long,
) // a chat can have multiple contacts
