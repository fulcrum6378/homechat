package ir.mahdiparastesh.homechat.Utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ir.mahdiparastesh.homechat.Network.HostBean;
import ir.mahdiparastesh.homechat.Network.NetInfo;

public class Export {

    private final List<HostBean> hosts;
    private final NetInfo net;

    public Export(Context ctxt, List<HostBean> hosts) {
        this.hosts = hosts;
        net = new NetInfo(ctxt);
        net.getWifiInfo();
    }

    public boolean writeToSd(String file) {
        String xml = prepareXml();
        try {
            FileWriter f = new FileWriter(file);
            f.write(xml);
            f.flush();
            f.close();
            return true;
        } catch (IOException e) {
            String TAG = "Export";
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    public String getFileName() {
        // TODO: Use getExternalFilesDir()
        return Environment.getExternalStorageDirectory().toString() + "/discovery-"
                + net.getNetIp() + ".xml";
    }

    private String prepareXml() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
                "<NetworkDiscovery>\r\n");
        // Network Information
        xml.append("\t<info>\r\n" + "\t\t<date>")
                .append((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")).format(new Date()))
                .append("</date>\r\n" // RFC 2822
                ).append("\t\t<network>").append(net.getNetIp()).append("/").append(net.cidr)
                .append("</network>\r\n").append("\t\t<ssid>").append(net.ssid)
                .append("</ssid>\r\n").append("\t\t<bssid>").append(net.bssid)
                .append("</bssid>\r\n").append("\t\t<ip>").append(net.ip)
                .append("</ip>\r\n").append("\t</info>\r\n");

        // Hosts
        if (hosts != null) {
            xml.append("\t<hosts>\r\n");
            for (int i = 0; i < hosts.size(); i++) {
                // Host info
                HostBean host = hosts.get(i);
                xml.append("\t\t<host ip=\"").append(host.ipAddress).append("\" mac=\"")
                        .append(host.hardwareAddress)
                        .append("\" vendor=\"")
                        .append(host.nicVendor)
                        .append("\">\r\n");
                // Open Ports //TODO: rething the XML structure to include close
                // and filtered ports
                if (host.portsOpen != null)
                    for (int port : host.portsOpen)
                        xml.append("\t\t\t<port>").append(port).append("/tcp open</port>\r\n");
                xml.append("\t\t</host>\r\n");
            }
            xml.append("\t</hosts>\r\n");
        }

        xml.append("</NetworkDiscovery>");
        return xml.toString();
    }

}
