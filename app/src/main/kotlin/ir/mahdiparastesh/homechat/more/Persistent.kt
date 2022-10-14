package ir.mahdiparastesh.homechat.more

import android.content.Context
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Model

interface Persistent {
    val c: Context
    var m: Model
    val dbLazy: Lazy<Database>
    val db: Database
    val dao: Database.DAO
}
