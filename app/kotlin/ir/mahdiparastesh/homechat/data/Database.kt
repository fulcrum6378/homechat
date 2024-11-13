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

        @Query("SELECT id FROM Contact")
        suspend fun contactIds(): List<Short>

        @Insert
        suspend fun addContact(item: Contact)

        @Update
        suspend fun updateContact(item: Contact)


        @Query("SELECT * FROM Chat ORDER BY (SELECT MAX(time) FROM Message WHERE chat = Chat.id) DESC")
        suspend fun chats(): List<Chat>

        @Query("SELECT id FROM Chat")
        suspend fun chatIds(): List<Short>

        @Query("SELECT * FROM Chat WHERE id LIKE :id LIMIT 1")
        suspend fun chat(id: Short): Chat

        @Insert
        suspend fun addChat(item: Chat)


        @Query("SELECT * FROM Message WHERE chat LIKE :chat ORDER BY time")
        suspend fun messages(chat: Short): List<Message>

        @Query("SELECT * FROM Message WHERE id LIKE :id AND chat LIKE :chat AND auth LIKE :auth LIMIT 1")
        suspend fun message(id: Long, chat: Short, auth: Short): Message?

        @Query("SELECT * FROM Message WHERE id IN (:ids) AND chat LIKE :chat AND auth LIKE :auth")
        suspend fun theseMessage(ids: List<Long>, chat: Short, auth: Short): List<Message>

        /*@Query("SELECT id FROM Message WHERE chat LIKE :chat")
        suspend fun messageIds(chat: Short): List<Long>*/

        @Insert(onConflict = OnConflictStrategy.ABORT)
        suspend fun addMessage(item: Message)

        @Update
        suspend fun updateMessage(item: Message)


        @Query("SELECT * FROM Seen WHERE msg LIKE :msg AND chat LIKE :chat")
        suspend fun seenForMessage(msg: Long, chat: Short): List<Seen>

        @Query("SELECT * FROM Seen WHERE msg LIKE :msg AND chat LIKE :chat AND contact LIKE :contact LIMIT 1")
        suspend fun seen(msg: Long, chat: Short, contact: Short): Seen?

        @Query("SELECT msg FROM Seen WHERE chat LIKE :chat AND contact LIKE -1 AND dateSeen IS NULL")
        suspend fun unseenInChat(chat: Short): List<Long>

        @Query("SELECT COUNT(*) FROM Seen WHERE chat LIKE :chat AND contact LIKE -1 AND dateSeen IS NULL")
        suspend fun countUnseenInChat(chat: Short): Int

        @Insert
        suspend fun addSeen(item: Seen)

        @Update
        suspend fun updateSeen(item: Seen)
    }

    companion object {
        fun build(c: Context) = Room
            .databaseBuilder(c, Database::class.java, "main.db")
            .fallbackToDestructiveMigration() //.addMigrations()
            .build()
        // You cannot use DB Browser for SQLite in order to manually "MODIFY TABLES"!!
        // Although you can make other kinds of editions.
    }
}
