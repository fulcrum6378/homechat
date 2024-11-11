package ir.mahdiparastesh.homechat

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.*
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import ir.mahdiparastesh.homechat.data.Database
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.data.Model
import ir.mahdiparastesh.homechat.data.Radar
import ir.mahdiparastesh.homechat.databinding.MainBinding
import ir.mahdiparastesh.homechat.more.Persistent
import ir.mahdiparastesh.homechat.page.PageCht
import ir.mahdiparastesh.homechat.page.PageSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class Main : AppCompatActivity(), Persistent, NavigationView.OnNavigationItemSelectedListener {

    override val c: Context get() = applicationContext
    override lateinit var m: Model
    override val dbLazy: Lazy<Database> = lazy { Database.build(c) }
    override val db: Database by dbLazy
    override val dao: Database.DAO by lazy { db.dao() }
    override lateinit var sp: SharedPreferences

    val b: MainBinding by lazy { MainBinding.inflate(layoutInflater) }
    lateinit var navHost: NavHostFragment
    lateinit var nav: NavController
    private val navMap = mapOf(
        R.id.navRadar to R.id.page_rad,
        R.id.navSettings to R.id.page_set,
    )

    private lateinit var nsdManager: NsdManager
    private lateinit var mServiceName: String
    private var registered = false
    private var discovering = false

    private val reqPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf()
    val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US/*TODO?*/)
    val timeFormat = SimpleDateFormat("HH:mm"/*:ss*/, Locale.US)

    companion object {
        const val SERVICE_TYPE = "_homechat._tcp."
        const val MSG_FOUND = 1
        const val MSG_LOST = 2
        const val MSG_NEW_MESSAGE = 3
        const val MSG_WIFI = 100
        const val EXTRA_OPEN_CHAT = "open_chat"
        var handler: Handler? = null
    }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        m = ViewModelProvider(this, Model.Factory())["Model", Model::class.java]
        m.aliveMain = true
        setContentView(b.root)
        sp = sp()

        // Navigation
        ActionBarDrawerToggle(
            this, b.root, b.toolbar, R.string.navOpen, R.string.navClose
        ).apply {
            b.root.addDrawerListener(this)
            isDrawerIndicatorEnabled = true
            syncState()
        }
        navHost = supportFragmentManager.findFragmentById(R.id.pager) as NavHostFragment
        nav = navHost.navController
        nav.addOnDestinationChangedListener { _, dest, _ ->
            b.nav.menu.children.forEach { it.isChecked = navMap[it.itemId] == dest.id }
        }
        b.nav.setNavigationItemSelectedListener(this)

        // Handler
        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_FOUND -> Device(msg.obj as NsdServiceInfo).apply {
                        if (name == mServiceName) m.radar.self = this
                        else {
                            CoroutineScope(Dispatchers.IO)
                                .launch { m.radar.insert(this@apply, dao) }
                            Sender.init(this@Main)
                        }
                    }
                    MSG_LOST -> (msg.obj as NsdServiceInfo).also { srvInfo ->
                        // don't wrap Device around it!
                        CoroutineScope(Dispatchers.IO)
                            .launch { m.radar.delete(srvInfo.serviceName, dao) }
                    }
                    MSG_NEW_MESSAGE -> {
                        // TODO in-app notifications
                    }
                    MSG_WIFI -> m.wifi.value = msg.obj as Boolean
                }
            }
        }

        // Register the service
        mServiceName = Settings.Global.getString(contentResolver, "device_name")
        if (!sp.contains(PageSet.PRF_PORT))
            sp.edit().putInt(PageSet.PRF_PORT, ServerSocket(0).use { it.localPort }).apply()
        if (!m.aliveReceiver) startService(Intent(c, Receiver::class.java))
        nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        nsdManager.registerService(NsdServiceInfo().apply {
            serviceName = mServiceName
            serviceType = SERVICE_TYPE
            port = sp.getInt(PageSet.PRF_PORT, -1)
            setAttribute(PageSet.PRF_UNIQUE, null)
        }, NsdManager.PROTOCOL_DNS_SD, regListener)

        // Request ignore battery optimizations
        // It must check for the ability of working in the background and also data in bg
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                )
            )
        } catch (_: ActivityNotFoundException) {
        }

        // ask for permissions
        reqPermissions.forEach {
            if (ActivityCompat.checkSelfPermission(c, it) != PackageManager.PERMISSION_GRANTED)
                reqPermLauncher.launch(it)
        }

        // monitor network connectivity
        m.wifi.observe(this) { wifi -> tbSubtitleListener.onRadarUpdated() }
        getSystemService(ConnectivityManager::class.java).registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(nw: Network, nwc: NetworkCapabilities) {}
                override fun onAvailable(network: Network) {
                    handler?.obtainMessage(MSG_WIFI, true)?.sendToTarget()
                }

                override fun onLost(network: Network) {
                    handler?.obtainMessage(MSG_WIFI, false)?.sendToTarget()
                }
            })
    }

    override fun setContentView(root: View?) {
        super.setContentView(root)
        root?.layoutDirection =
            if (!resources.getBoolean(R.bool.dirRtl)) ViewGroup.LAYOUT_DIRECTION_LTR
            else ViewGroup.LAYOUT_DIRECTION_RTL
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        nav.navigate(navMap[item.itemId]!!)
        b.root.closeDrawer(GravityCompat.START)
        return true
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

    private var firstResume = true
    override fun onResume() {
        super.onResume()
        val mFirstResume = firstResume

        // The subtitle of the toolbar
        m.radar.updateListeners.add(tbSubtitleListener)

        // NSD
        startDiscovery()
        CoroutineScope(Dispatchers.IO).launch {
            if (m.contacts == null) m.contacts = CopyOnWriteArrayList(dao.contacts())
            if (m.chats == null) m.chats = CopyOnWriteArrayList(dao.chats())
            m.radar.update(dao)
            if (mFirstResume && intent.hasExtra(EXTRA_OPEN_CHAT)) withContext(Dispatchers.Main) {
                nav.navigate(
                    R.id.action_page_rad_to_page_cht,
                    bundleOf(PageCht.ARG_CHAT_ID to intent.getShortExtra(EXTRA_OPEN_CHAT, 0))
                )
            }
        }

        firstResume = false
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
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && SdkExtensions.getExtensionVersion(
                        Build.VERSION_CODES.TIRAMISU
                    ) >= 7
                ) nsdManager.registerServiceInfoCallback(srvInfo, null, serviceInfoCallback)*/
                @Suppress("DEPRECATION")
                nsdManager.resolveService(srvInfo, resolveListener)
            } catch (e: IllegalArgumentException) {
                if (e.message != "listener already in use")
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

    private val resolveListener = object : NsdManager.ResolveListener {
        @WorkerThread
        override fun onServiceResolved(srvInfo: NsdServiceInfo) {
            handler?.obtainMessage(MSG_FOUND, srvInfo)?.sendToTarget()
        }

        override fun onResolveFailed(srvInfo: NsdServiceInfo, errorCode: Int) {
            Toast.makeText(c, "onResolveFailed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    /*private val serviceInfoCallback = @RequiresExtension(
        extension = Build.VERSION_CODES.TIRAMISU, version = 7
    ) object : NsdManager.ServiceInfoCallback {
        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {}
        override fun onServiceUpdated(serviceInfo: NsdServiceInfo) {}
        override fun onServiceLost() {}
        override fun onServiceInfoCallbackUnregistered() {}
    }*/

    /** Requests all the required permissions. (currently only for notifications in Android 13+) */
    private val reqPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /*isGranted ->*/ }

    val tbSubtitleListener = object : Radar.OnUpdateListener {
        override fun onRadarUpdated() {
            if (m.wifi.value == false) {
                b.toolbar.setSubtitle(R.string.noNetwork)
                return
            }
            b.toolbar.subtitle = when (nav.currentDestination?.id) {
                R.id.page_cht -> (navHost.childFragmentManager.fragments[0] as PageCht)
                    .chat.onlineStatus(m.radar)// ?: ""
                else -> ""
            }
        }
    }

    override fun onPause() {
        super.onPause()

        // NSD
        if (discovering) nsdManager.stopServiceDiscovery(discoveryListener)

        // The subtitle of the toolbar
        m.radar.updateListeners.remove(tbSubtitleListener)
    }

    override fun onDestroy() {
        if (registered) nsdManager.unregisterService(regListener)
        handler = null
        m.aliveMain = false
        // if (dbLazy.isInitialized() && m.anyPersistentAlive()) db.close()
        super.onDestroy()
    }

    /*@ColorInt
    fun themeColor(@AttrRes attr: Int) =
        TypedValue().apply { theme.resolveAttribute(attr, this, true) }.data*/
}

/* TODO
  * A device with VPN cannot receive, but can send to a VPN-less device!
  * Fucks up when 2 devices open simultaneously!
  * It doesn't store the self's message on a simultaneous send.
  * Regularly init the Queuer
  * Typing status
  * https://developer.android.com/develop/ui/views/notifications/bubbles
  * https://developer.android.com/develop/ui/views/components/settings/organize-your-settings
  * Crashes when you turn of the internet while chatting (Radar::update())
  * Contact::lastOnline
 */
