package ir.mahdiparastesh.homechat.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Room
import androidx.room.RoomDatabase

@androidx.room.Database(
    entities = [
        Contact::class, Chat::class, Message::class
    ], version = 1, exportSchema = false
)
abstract class Database : RoomDatabase() {
    abstract fun dao(): DAO

    @Dao
    interface DAO {
    }

    companion object {
        fun build(c: Context) = Room
            .databaseBuilder(c, Database::class.java, "main.db")
            //.addMigrations()
            .build()
    }
}
