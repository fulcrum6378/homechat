package ir.mahdiparastesh.homechat.data

import androidx.room.*

@androidx.room.Database(
    entities = [
        Contact::class, Chat::class, Message::class, Seen::class, Binary::class
    ], version = 1, exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun dao(): DAO

    @Dao
    interface DAO {
        @Query("SELECT * FROM contact")
        suspend fun contacts(): List<Contact>

        @Query("SELECT id FROM contact")
        suspend fun contactIds(): List<Short>

        @Insert
        suspend fun addContact(item: Contact)

        @Update
        suspend fun updateContact(item: Contact)


        @Query("SELECT * FROM chat ORDER BY pinned DESC, (SELECT MAX(time) FROM message WHERE chat = chat.id) DESC")
        suspend fun chats(): List<Chat>

        @Query("SELECT id FROM chat")
        suspend fun chatIds(): List<Short>

        @Query("SELECT * FROM chat WHERE id LIKE :id LIMIT 1")
        suspend fun chat(id: Short): Chat

        @Insert
        suspend fun addChat(item: Chat)

        @Update
        suspend fun updateChat(item: Chat)


        @Query("SELECT * FROM message WHERE chat LIKE :chat ORDER BY time")
        suspend fun messages(chat: Short): List<Message>

        @Query("SELECT * FROM message WHERE id LIKE :id AND chat LIKE :chat AND auth LIKE :auth LIMIT 1")
        suspend fun message(id: Long, chat: Short, auth: Short): Message?

        @Query("SELECT * FROM message WHERE id IN (:ids) AND chat LIKE :chat AND auth LIKE :auth")
        suspend fun theseMessage(ids: List<Long>, chat: Short, auth: Short): List<Message>

        /*@Query("SELECT id FROM message WHERE chat LIKE :chat")
        suspend fun messageIds(chat: Short): List<Long>*/

        @Insert(onConflict = OnConflictStrategy.ABORT)
        suspend fun addMessage(item: Message)

        /*@Update
        suspend fun updateMessage(item: Message)*/


        @Query("SELECT * FROM seen WHERE msg LIKE :msg AND chat LIKE :chat")
        suspend fun seenForMessage(msg: Long, chat: Short): List<Seen>

        @Query("SELECT * FROM seen WHERE msg LIKE :msg AND chat LIKE :chat AND contact LIKE :contact LIMIT 1")
        suspend fun seen(msg: Long, chat: Short, contact: Short): Seen?

        @Query("SELECT msg FROM seen WHERE chat LIKE :chat AND contact LIKE -1 AND seen_at IS NULL")
        suspend fun unseenInChat(chat: Short): List<Long>

        @Query("SELECT COUNT(*) FROM Seen WHERE chat LIKE :chat AND contact LIKE -1 AND seen_at IS NULL")
        suspend fun countUnseenInChat(chat: Short): Int

        @Insert
        suspend fun addSeen(item: Seen)

        @Update
        suspend fun updateSeen(item: Seen)


        @Insert
        suspend fun addBinary(item: Binary): Long
    }
}
