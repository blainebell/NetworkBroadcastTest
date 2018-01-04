package ravn.networkbroadcasttest;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by bell on 12/24/17.
 */

public class AndroidSocketUtils {
    public static String getBroadcastAddress(Activity activity) {
        try {
            WifiManager wifi = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            DhcpInfo dhcp = wifi.getDhcpInfo();
            // handle null somehow
            int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
            byte[] quads = new byte[4];
            for (int k = 0; k < 4; k++)
                quads[k] = (byte) (broadcast >> (k * 8));
            String hostAddress = InetAddress.getByAddress(quads).getHostAddress();
            return hostAddress;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
