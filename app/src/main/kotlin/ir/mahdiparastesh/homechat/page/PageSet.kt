package ir.mahdiparastesh.homechat.page

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import ir.mahdiparastesh.homechat.R

class PageSet : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    companion object {
        const val PRF_PORT = "port"
    }
}
