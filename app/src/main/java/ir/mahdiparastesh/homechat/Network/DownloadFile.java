// Taken from: http://github.com/ctrlaltdel/TahoeLAFS-android

package ir.mahdiparastesh.homechat.Network;

import ir.mahdiparastesh.homechat.ActivityMain;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;


import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class DownloadFile {

    private static final String TAG = "DownloadFile";
    //TODO MAHDI private final HttpClient httpclient;

    public DownloadFile(final Context ctxt, String url, FileOutputStream out) throws IOException, NullPointerException {
        String version = "0.3.x";
        try {
            version = ctxt.getPackageManager().getPackageInfo(ActivityMain.TAG, 0).versionName;
        } catch (NameNotFoundException ignored) {
        }

        //TODO MAHDI httpclient = HttpClientBuilder.create().build();
        String USERAGENT = "Android/" + android.os.Build.VERSION.RELEASE + " ("
                + android.os.Build.MODEL + ") NetworkDiscovery/";
        //TODO MAHDI httpclient.getParams().setParameter("http.useragent", USERAGENT + version);
        InputStream in = openURL(url);
        if (in == null) {
            Log.e(TAG, "Unable to download: " + url);
            return;
        }

        final ReadableByteChannel inputChannel = Channels.newChannel(in);
        final WritableByteChannel outputChannel = Channels.newChannel(out);

        try {
            Log.i(TAG, "Downloading " + url);
            fastChannelCopy(inputChannel, outputChannel);
        } finally {
            try {
                if (inputChannel != null) {
                    inputChannel.close();
                }
                if (outputChannel != null) {
                    outputChannel.close();
                }
                in.close();
                if (out != null) out.close();
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    // TODO MAHDI Log.e(TAG, e.getMessage());
                } else Log.e(TAG, "fastChannelCopy() unknown error");
            }
        }
    }

    private InputStream openURL(String url) {
        /*TODO MAHDI HttpGet httpget = new HttpGet(url);
        HttpResponse response;
        try {
            try {
                response = httpclient.execute(httpget);
            } catch (SSLException e) {
                Log.i(TAG, "SSL Certificate is not trusted");
                response = httpclient.execute(httpget);
            }
            //TODO MAHDI Log.i(TAG, "Status:[" + response.getStatusLine().toString() + "]");
            *//*TODO MAHDI HttpEntity entity = response.getEntity();

            if (entity != null) return new GZIPInputStream(entity.getContent());*//*
        } catch (ClientProtocolException e) {
            Log.e(TAG, "There was a protocol based error", e);
        } catch (UnknownHostException e) {
            // TODO MAHDI Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "There was an IO Stream related error", e);
        }*/

        return null;
    }

    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest)
            throws IOException, NullPointerException {
        if (src != null && dest != null) {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
            while (src.read(buffer) != -1) {
                // prepare the buffer to be drained
                buffer.flip();
                // write to the channel, may block
                dest.write(buffer);
                // If partial transfer, shift remainder down
                // If buffer is empty, same as doing clear()
                buffer.compact();
            }
            // EOF will leave buffer in fill state
            buffer.flip();
            // make sure the buffer is fully drained.
            while (buffer.hasRemaining())
                dest.write(buffer);
        }
    }
}
