package ir.mahdiparastesh.homechat.util

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.page.PageCht
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityLaunchers(c: Main) {
    var pageCht: PageCht? = null

    val requestPermissions = c.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /*isGranted ->*/ }

    val attach: ActivityResultLauncher<Intent> = c.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK && pageCht != null)
            CoroutineScope(Dispatchers.IO).launch { pageCht?.attach(it.data!!) }
    }
}
