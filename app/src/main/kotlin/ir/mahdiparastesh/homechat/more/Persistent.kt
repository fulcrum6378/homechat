package ir.mahdiparastesh.homechat.more

import android.content.Context
import android.content.SharedPreferences
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model

interface Persistent {
    val c: Context
    val m: Model
    val dbLazy: Lazy<Database>
    val db: Database
    val dao: Database.DAO
    val sp: SharedPreferences

    fun Context.sp(): SharedPreferences =
        getSharedPreferences("settings", Context.MODE_PRIVATE)
}
