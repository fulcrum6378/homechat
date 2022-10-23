package ir.mahdiparastesh.homechat.data

import android.content.Context
import androidx.room.*
import java.util.*

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

        @Query("SELECT id FROM Contact")
        suspend fun contactIds(): List<Short>

        @Insert // (onConflict = OnConflictStrategy.REPLACE)
        suspend fun addContact(item: Contact)


        @Query("SELECT * FROM Chat")
        suspend fun chats(): List<Chat>

        @Query("SELECT id FROM Chat")
        suspend fun chatIds(): List<Short>

        @Insert
        suspend fun addChat(item: Chat)


        @Query("SELECT * FROM Message WHERE chat LIKE :chat")
        suspend fun messages(chat: Short): List<Message>

        @Query("SELECT * FROM Message WHERE id LIKE :id AND chat LIKE :chat LIMIT 1")
        suspend fun message(id: Long, chat: Short): Message?

        @Insert
        suspend fun addMessage(item: Message)


        @Query("SELECT * FROM Seen WHERE msg LIKE :msg AND chat LIKE :chat AND contact LIKE :contact LIMIT 1")
        suspend fun seen(msg: Long, chat: Short, contact: Short): Seen?

        @Insert
        suspend fun addSeen(item: Seen)

        @Update
        suspend fun updateSeen(item: Seen)
    }

    companion object {
        fun build(c: Context) = Room
            .databaseBuilder(c, Database::class.java, "main.db")
            // .addMigrations()
            .build()
        // You cannot use DB Browser for SQLite in order to manually "MODIFY TABLES"!!
        // Although you can make other kinds of editions.

        fun now() = Calendar.getInstance().timeInMillis

        fun Long.calendar(): Calendar = // from milliseconds
            Calendar.getInstance().apply { timeInMillis = this@calendar }
    }
}
