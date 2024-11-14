package ir.mahdiparastesh.homechat.base

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.room.Room
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.page.PageSet

interface Persistent {
    val c: Context
    val m: Model
    val dbLazy: Lazy<Database>
    val db: Database
    val dao: Database.DAO
    val sp: SharedPreferences

    fun model() =
        ViewModelProvider(this as ViewModelStoreOwner, Model.Factory())["Model", Model::class.java]

    fun database() = Room
        .databaseBuilder(c, Database::class.java, "main.db")
        .build()

    fun Context.sp(): SharedPreferences =
        getSharedPreferences(PageSet.SP_NAME, Context.MODE_PRIVATE)
}
