package ir.mahdiparastesh.homechat.Network;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.net.wifi.WifiManager;

import java.net.NetworkInterface;
import java.util.Enumeration;

import ir.mahdiparastesh.homechat.Utils.Db;

public class HardwareAddress {

    private final static String TAG = "HardwareAddress";
    private final static String REQ = "select vendor from oui where mac=?";

    public HardwareAddress() {
    }

    // Find the MAC address
    public static String getHardwareAddress(Context context, String ip) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces.hasMoreElements())
                return new String(interfaces.nextElement().getHardwareAddress());
        } catch (Exception e) {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            return wifiManager.getConnectionInfo().getMacAddress();
        }
        return null;
    }

    public static String getNicVendor(String hw) throws SQLiteDatabaseCorruptException {
        String ni = null;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                    Db.PATH + Db.DB_NIC, null,
                    SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            if (db != null) {
                // Db request
                if (db.isOpen()) {
                    Cursor c = db.rawQuery(REQ, new String[]{hw.replace(":", "")
                            .substring(0, 6).toUpperCase()});
                    if (c.moveToFirst()) {
                        ni = c.getString(0);
                    }
                    c.close();
                }
                db.close();
            }
        } catch (IllegalStateException | SQLiteException e) {
            // TODO MAHDI Log.e(TAG, e.getMessage());
        } // FIXME: Reset db
        //Context ctxt = d.getApplicationContext();
        //Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
        //edit.putInt(Prefs.KEY_RESET_NICDB, 1);
        //edit.apply();

        return ni;
    }
}
