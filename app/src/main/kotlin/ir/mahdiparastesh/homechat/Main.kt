package ir.mahdiparastesh.homechat

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.Toast
import ir.mahdiparastesh.homechat.data.Device
import ir.mahdiparastesh.homechat.databinding.MainBinding
import ir.mahdiparastesh.homechat.more.BaseActivity
import java.net.ServerSocket

class Main : BaseActivity() {
    private val b: MainBinding by lazy { MainBinding.inflate(layoutInflater) }
    private val nsdManager: NsdManager by lazy { getSystemService(Context.NSD_SERVICE) as NsdManager }
    private var mServiceName = SERVICE_NAME
    private var mServicePort = 0
    private var registered = false
    private var discovering = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    HANDLE_FOUND, HANDLE_LOST -> updateDevices()
                }
            }
        }

        // Register the service (https://developer.android.com/training/connect-devices-wirelessly/nsd)
        mServicePort = ServerSocket(0).use { it.localPort }
        nsdManager.registerService(NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = mServicePort
        }, NsdManager.PROTOCOL_DNS_SD, regListener)
    }

    private val regListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            registered = true
            // Android may have changed the service name in order to resolve a conflict!
            mServiceName = serviceInfo.serviceName
            startDiscovery()
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
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
            if (srvInfo.serviceType == SERVICE_TYPE && srvInfo.serviceName.startsWith(SERVICE_NAME))
                nsdManager.resolveService(srvInfo, resolveListener)
            else Toast.makeText(
                c,
                "improper -> ${srvInfo.serviceType} ${srvInfo.serviceName}", Toast.LENGTH_LONG
            ).show()
        }

        override fun onServiceLost(srvInfo: NsdServiceInfo) {
            m.radar.removeAll { it.service == srvInfo.serviceName }
            handler?.obtainMessage(HANDLE_LOST)?.sendToTarget()
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
            m.radar.add(Device(srvInfo, mServiceName))
            handler?.obtainMessage(HANDLE_FOUND)?.sendToTarget()
        }

        override fun onResolveFailed(srvInfo: NsdServiceInfo, errorCode: Int) {
            Toast.makeText(c, "onResolveFailed: $errorCode", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateDevices() {
        b.test.text = StringBuilder().apply {
            for (dev: Device in m.radar)
                append(dev.toString()).append(" (${dev.service})\n")
        }.toString()
    }

    override fun onPause() {
        super.onPause()
        if (discovering) nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (registered) nsdManager.unregisterService(regListener)
        handler = null
    }

    companion object {
        const val SERVICE_NAME = "HomeChat"
        const val SERVICE_TYPE = "_homechat._tcp."
        const val HANDLE_FOUND = 1
        const val HANDLE_LOST = 2
        var handler: Handler? = null
    }
}
