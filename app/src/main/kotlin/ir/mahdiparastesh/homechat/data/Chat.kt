package ir.mahdiparastesh.homechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class Chat(
    var contacts: String, // separated by ","
) {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
}

/*
* A chat can have multiple contacts.
* Remember that this is a first-person database;
* the user will be exempted from all viewing statuses. TODO BUT IT'S NOT POSSIBLE
 */
