package ir.mahdiparastesh.homechat.Network;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Banner extends AsyncTask<Void, String, Void> {

    private static final int BUF = 8 * 1024;
    private final String host;
    private final int port;
    private final int timeout;

    public Banner(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Socket s = new Socket();
            s.bind(null);
            s.connect(new InetSocketAddress(host, port), timeout);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()), BUF);
            String banner = "";
            while ((banner = in.readLine()) != null) {
                break;
            }
            in.close();
            s.close();
            String TAG = "Banner";
            Log.v(TAG, banner);
            return null;
        } catch (IOException ignored) {
        }
        return null;
    }
}
