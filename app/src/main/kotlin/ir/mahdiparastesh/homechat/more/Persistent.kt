package ir.mahdiparastesh.homechat.more

import android.content.Context
import android.content.SharedPreferences
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

    fun Context.sp(): SharedPreferences =
        getSharedPreferences(PageSet.SP_NAME, Context.MODE_PRIVATE)
}
