package ir.mahdiparastesh.homechat;

import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;

import ir.mahdiparastesh.homechat.Network.HardwareAddress;
import ir.mahdiparastesh.homechat.Network.HostBean;
import ir.mahdiparastesh.homechat.Network.NetInfo;
import ir.mahdiparastesh.homechat.Utils.Prefs;

public class DnsDiscovery extends AbstractDiscovery {

    public DnsDiscovery(ActivityDiscovery discover) {
        super(discover);
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mDiscover == null) return null;
        final ActivityDiscovery discover = mDiscover.get();
        if (discover == null) return null;

        String TAG = "DnsDiscovery";
        Log.i(TAG, "start=" + NetInfo.getIpFromLongUnsigned(start) + " (" + start
                + "), end=" + NetInfo.getIpFromLongUnsigned(end) + " (" + end
                + "), length=" + size);

        int timeout = Integer.parseInt(discover.prefs.getString(Prefs.KEY_TIMEOUT_DISCOVER,
                Prefs.DEFAULT_TIMEOUT_DISCOVER));
        Log.i(TAG, "timeout=" + timeout + "ms");

        for (long i = start; i < end + 1; i++) {
            hosts_done++;
            HostBean host = new HostBean();
            host.ipAddress = NetInfo.getIpFromLongUnsigned(i);
            try {
                InetAddress ia = InetAddress.getByName(host.ipAddress);
                host.hostname = ia.getCanonicalHostName();
                host.isAlive = ia.isReachable(timeout) ? 1 : 0;
            } catch (IOException e) {
                // TODO MAHDI Log.e(TAG, e.getMessage());
            }
            if (host.hostname != null && !host.hostname.equals(host.ipAddress)) {
                // Is gateway ?
                if (discover.net.gatewayIp.equals(host.ipAddress)) {
                    host.deviceType = 1;
                }
                // Mac Addr
                host.hardwareAddress = HardwareAddress.getHardwareAddress(
                        mDiscover.get(), host.ipAddress);
                // NIC vendor
                try {
                    host.nicVendor = HardwareAddress.getNicVendor(host.hardwareAddress);
                } catch (SQLiteDatabaseCorruptException e) {
                    // TODO MAHDI Log.e(TAG, e.getMessage());
                }
                publishProgress(host);
            } else publishProgress((HostBean) null);
        }
        return null;
    }
}
