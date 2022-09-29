package ir.mahdiparastesh.homechat.Network;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RateControl {

    private static final int BUF = 512;
    private final int REACH_TIMEOUT = 5000;
    private final Pattern mPattern;
    public String indicator = null;
    public int rate = 800; // Slow start

    public RateControl() {
        String PTN = "^rtt min/avg/max/mdev = [0-9.]+/[0-9.]+/([0-9.]+)/[0-9.]+ ms.*";
        mPattern = Pattern.compile(PTN);
    }

    public void adaptRate() {
        int response_time;
        if ((response_time = getAvgResponseTime(indicator)) > 0) {
            if (response_time > 100) // Most distanced hosts
                rate = response_time * 5; // Minimum 500ms
            else
                rate = response_time * 10; // Maximum 1000ms
            if (rate > REACH_TIMEOUT) rate = REACH_TIMEOUT;
        }
    }

    private int getAvgResponseTime(String host) {
        // TODO: Reduce allocation
        BufferedReader reader = null;
        Matcher matcher;
        try {
            String CMD = "/system/bin/ping -A -q -n -w 3 -W 2 -c 3 ";
            final Process proc = Runtime.getRuntime().exec(CMD + host);
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()), BUF);
            String line;
            while ((line = reader.readLine()) != null) {
                matcher = mPattern.matcher(line);
                if (matcher.matches()) {
                    reader.close();
                    return (int) Float.parseFloat(Objects.requireNonNull(matcher.group(1)));
                }
            }
            reader.close();
        } catch (Exception e) {
            String TAG = "RateControl";
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
            try {
                final long start = System.nanoTime();
                if (InetAddress.getByName(host).isReachable(REACH_TIMEOUT)) {
                    Log.i(TAG, "Using Java ICMP request instead ...");
                    return (int) ((System.nanoTime() - start) / 1000);
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getMessage());
            }
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ignored) {
            }
        }
        return rate;
    }
}
