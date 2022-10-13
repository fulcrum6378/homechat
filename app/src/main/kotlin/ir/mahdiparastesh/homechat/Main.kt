package ir.mahdiparastesh.homechat

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import ir.mahdiparastesh.homechat.data.Contact
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.databinding.MainBinding
import ir.mahdiparastesh.homechat.more.Persistent
import java.net.ServerSocket

class Main : AppCompatActivity(), Persistent, NavigationView.OnNavigationItemSelectedListener {
    private val b: MainBinding by lazy { MainBinding.inflate(layoutInflater) }
    lateinit var nav: NavController
    private val navMap = mapOf(
        R.id.navRadar to R.id.page_rad,
        R.id.navSettings to R.id.page_set,
    )

    private lateinit var nsdManager: NsdManager
    private lateinit var mServiceName: String
    private var mServicePort = 0
    private var registered = false
    private var discovering = false
    private lateinit var antennaIntent: Intent

    override val c: Context get() = applicationContext
    override lateinit var m: Model
    override val dbLazy: Lazy<Database> = lazy { Database.build(c) }
    override val db: Database by dbLazy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
        m.aliveMain = true
        setContentView(b.root)

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: android.os.Message) {
                when (msg.what) {
                    MSG_FOUND -> (msg.obj as NsdServiceInfo).also { srvInfo ->
                        val dev = Device(srvInfo, mServiceName)
                        if (m.radar.value?.contains(dev) == true) return
                        m.radar.value = m.radar.value?.plus(dev)
                            ?.sortedBy { it.name }?.sortedBy { !it.isMe }
                        /*if (dev.isMe)*/ startService(antennaIntent)
                    }
                    MSG_LOST -> (msg.obj as NsdServiceInfo).also { srvInfo ->
                        // don't wrap Device around it!
                        m.radar.value = m.radar.value?.filter { it.name != srvInfo.serviceName }
                    }
                    3 -> Toast.makeText(
                        c, "${msg.obj}",
                        if (msg.arg1 == 1) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Register the service (https://developer.android.com/training/connect-devices-wirelessly/nsd)
        mServiceName = Settings.Global.getString(contentResolver, "device_name")
        mServicePort = ServerSocket(0).use { it.localPort }
        antennaIntent = Intent(c, Antenna::class.java).putExtra(Antenna.EXTRA_PORT, mServicePort)
        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager.registerService(NsdServiceInfo().apply {
            serviceName = mServiceName
            serviceType = SERVICE_TYPE
            port = mServicePort
            setAttribute(Contact.ATTR_EMAIL, null) // TODO
            setAttribute(Contact.ATTR_PHONE, null) // TODO
        }, NsdManager.PROTOCOL_DNS_SD, regListener)

        // Navigation
        ActionBarDrawerToggle(
            this, b.root, b.toolbar, R.string.navOpen, R.string.navClose
        ).apply {
            b.root.addDrawerListener(this)
            isDrawerIndicatorEnabled = true
            syncState()
        }
        nav = (supportFragmentManager.findFragmentById(R.id.pager) as NavHostFragment).navController
        nav.navigate(R.id.page_rad)
        /*nav.addOnDestinationChangedListener { _, dest, _ -> TODO
            b.nav.menu.forEach { it.isChecked = navMap[it.itemId] == dest.id }
        }*/
        b.nav.setNavigationItemSelectedListener(this)

        // Test
        /*CoroutineScope(Dispatchers.IO).launch {
            db.dao().addContact(Contact(5, "Lashi", "192.168.1.0"))
        }*/
    }

    override fun setContentView(root: View?) {
        super.setContentView(root)
        root?.layoutDirection =
            if (!resources.getBoolean(R.bool.dirRtl)) ViewGroup.LAYOUT_DIRECTION_LTR
            else ViewGroup.LAYOUT_DIRECTION_RTL
    }

    private val regListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(srvInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceRegistered(srvInfo: NsdServiceInfo) {
            registered = true
            // Android may have changed the service name in order to resolve a conflict!
            mServiceName = srvInfo.serviceName
            startDiscovery()
        }

        override fun onUnregistrationFailed(srvInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(srvInfo: NsdServiceInfo) {
            registered = false
        }
    }

    override fun onResume() {
        super.onResume()
        startDiscovery()
    }

    private fun startDiscovery() {
        if (discovering) return
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            discovering = true
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Toast.makeText(c, "onStartDiscoveryFailed: $errorCode", Toast.LENGTH_LONG).show()
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onServiceFound(srvInfo: NsdServiceInfo) {
            if (srvInfo.serviceType == SERVICE_TYPE) try {
                nsdManager.resolveService(srvInfo, resolveListener)
            } catch (e: IllegalArgumentException) { // "listener already in use"
                Toast.makeText(c, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServiceLost(srvInfo: NsdServiceInfo) {
            handler?.obtainMessage(MSG_LOST, srvInfo)?.sendToTarget()
        }

        override fun onDiscoveryStopped(serviceType: String) {
            discovering = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Toast.makeText(c, "onStopDiscoveryFailed: $errorCode", Toast.LENGTH_LONG).show()
            nsdManager.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener { // not UI thread
        override fun onServiceResolved(srvInfo: NsdServiceInfo) {
            handler?.obtainMessage(MSG_FOUND, srvInfo)?.sendToTarget()
        }

        override fun onResolveFailed(srvInfo: NsdServiceInfo, errorCode: Int) {
            Toast.makeText(c, "onResolveFailed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        nav.navigate(navMap[item.itemId]!!); return true
    }

    override fun onPause() {
        super.onPause()
        if (discovering) nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun onDestroy() {
        if (registered) nsdManager.unregisterService(regListener)
        stopService(antennaIntent)
        handler = null
        m.aliveMain = false
        if (dbLazy.isInitialized() && m.anyPersistentAlive()) db.close()
        super.onDestroy()
    }

    companion object {
        const val SERVICE_TYPE = "_homechat._tcp."
        const val MSG_FOUND = 1
        const val MSG_LOST = 2
        var handler: Handler? = null
    }
}

/* TODO
* Cannot work with VPN!
* Fucks up when 2 devices open simultaneously!
*/
