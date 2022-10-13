package ir.mahdiparastesh.homechat.data

import android.content.Context
import androidx.room.*

@androidx.room.Database(
    entities = [
        Contact::class, Chat::class, Message::class, Seen::class
    ], version = 1, exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun dao(): DAO

    @Dao
    interface DAO {
        @Query("SELECT * FROM Contact")
        suspend fun contacts(): List<Contact>

        @Query("SELECT * FROM Chat")
        suspend fun chats(): List<Chat>


        @Insert
        suspend fun addContact(item: Contact)
    }

    companion object {
        fun build(c: Context) = Room
            .databaseBuilder(c, Database::class.java, "main.db")
            //.addMigrations()
            .build()
    }
}
