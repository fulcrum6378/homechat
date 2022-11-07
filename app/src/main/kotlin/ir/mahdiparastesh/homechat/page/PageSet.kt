package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ir.mahdiparastesh.homechat.R

class PageSet : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = SP_NAME
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    companion object {
        const val SP_NAME = "settings"
        const val PRF_UNIQUE = "unique"

        // Hidden
        const val PRF_PORT = "port"
    }
}
