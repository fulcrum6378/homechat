package ir.mahdiparastesh.homechat

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.widget.Toast
import ir.mahdiparastesh.homechat.databinding.MainBinding
import ir.mahdiparastesh.homechat.more.BaseActivity
import java.net.ServerSocket

class Main : BaseActivity() {
    private val b: MainBinding by lazy { MainBinding.inflate(layoutInflater) }
    private val nsdManager: NsdManager by lazy { getSystemService(Context.NSD_SERVICE) as NsdManager }
    private var mServiceName = SERVICE_NAME
    private var mServicePort = 0

    companion object {
        const val SERVICE_NAME = "HomeChat"
        const val SERVICE_TYPE = "_homechat._tcp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(b.root)

        // Register the service
        ServerSocket(0).use { socket -> mServicePort = socket.localPort }
        Toast.makeText(c, "ServerSocket: $mServicePort", Toast.LENGTH_SHORT).show()
        nsdManager.registerService(NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = mServicePort
        }, NsdManager.PROTOCOL_DNS_SD, regListener)
    }

    private val regListener = object : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            // Android may have changed the service name in order to resolve a conflict!
            mServiceName = serviceInfo.serviceName
            Toast.makeText(c, "onServiceRegistered: $mServiceName", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Toast.makeText(c, "onStartDiscoveryFailed: $errorCode", Toast.LENGTH_LONG).show()
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onServiceFound(srvInfo: NsdServiceInfo) {
            if (srvInfo.serviceType == SERVICE_TYPE && srvInfo.serviceName.startsWith(SERVICE_NAME))
                m.discoveryResults.add(srvInfo)
            Toast.makeText(
                c, "${srvInfo.serviceName} ${srvInfo.serviceType} " +
                        "(${srvInfo.host}:${srvInfo.port})", Toast.LENGTH_LONG
            ).show()
        }

        override fun onServiceLost(srvInfo: NsdServiceInfo) {
            m.discoveryResults.remove(srvInfo)
            Toast.makeText(
                c, "${srvInfo.serviceName} ${srvInfo.serviceType} " +
                        "(${srvInfo.host}:${srvInfo.port})", Toast.LENGTH_LONG
            ).show()
        }

        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Toast.makeText(c, "onStopDiscoveryFailed: $errorCode", Toast.LENGTH_LONG).show()
            nsdManager.stopServiceDiscovery(this)
        }
    }

    /*private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")

            if (serviceInfo.serviceName == mServiceName) {
                Log.d(TAG, "Same IP.")
                return
            }
            mService = serviceInfo
            val port: Int = serviceInfo.port
            val host: InetAddress = serviceInfo.host
        }
    }*/
    // nsdManager.resolveService(service, resolveListener)
    // https://developer.android.com/training/connect-devices-wirelessly/nsd

    override fun onPause() {
        super.onPause()
        nsdManager.stopServiceDiscovery(discoveryListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdManager.unregisterService(regListener)
    }
}
