package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ir.mahdiparastesh.homechat.Main
import ir.mahdiparastesh.homechat.R

class PageSet : PreferenceFragmentCompat() {
    private val c: Main by lazy { activity as Main }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = SP_NAME
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onResume() {
        super.onResume()
        c.b.toolbar.setTitle(R.string.settings)
    }

    companion object {
        const val SP_NAME = "settings"
        const val PRF_UNIQUE = "unique"

        // Hidden
        const val PRF_PORT = "port"
    }
}
