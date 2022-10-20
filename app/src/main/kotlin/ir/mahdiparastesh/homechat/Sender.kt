package ir.mahdiparastesh.homechat

import android.content.Intent
import ir.mahdiparastesh.homechat.more.WiseService

class Sender : WiseService() {

    override fun onCreate() {
        super.onCreate()
        m.aliveSender = true
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        m.aliveSender = false
        super.onDestroy()
    }

    companion object
}
