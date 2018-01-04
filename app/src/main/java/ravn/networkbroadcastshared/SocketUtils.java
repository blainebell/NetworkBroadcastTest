package ravn.networkbroadcastshared;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by bell on 12/23/17.
 */

public class SocketUtils {
    static Pattern IPV4_PATTERN = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");
    public static boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
    static public Pair<NetworkInterface,String> getLocalNetworkInterfaceInfo(){
        NetworkInterface retni = null;
        String ipAddress = "";
        int prefixLength = 0;
        try {
            Enumeration<NetworkInterface> lni = NetworkInterface.getNetworkInterfaces();
            while(lni.hasMoreElements()){
                NetworkInterface ni = lni.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;
                List<InterfaceAddress> lia = ni.getInterfaceAddresses();
                for (InterfaceAddress ia : lia){
                    int npl = ia.getNetworkPrefixLength();
                    String hostAddress = ia.getAddress().getHostAddress();
                    if (!isIPv4Address(hostAddress))
                        continue;
                    if ( !hostAddress.equals("127.0.0.1") && npl >= prefixLength) { //( npl > 0 && npl <= 24)){// && (npl % 8) == 0 ){
                        ipAddress = hostAddress;
                        prefixLength = npl;
                        retni = ni;
                    }
                }
            }
        } catch (SocketException se){
            se.printStackTrace();
        }
        return new Pair<NetworkInterface,String>(retni, ipAddress);
    }
    static public String getLocalIPAddress(){
        return getLocalNetworkInterfaceInfo().second;
    }
    static public NetworkInterface getLocalNetworkInterface(){
        return getLocalNetworkInterfaceInfo().first;
    }
    static public String getBroadcastAddress() {
    		//System.out.println("getBroadcastAddress() called");
		NetworkInterface ni = SocketUtils.getLocalNetworkInterfaceInfo().first;
		if (ni==null){
			System.out.println("WARNING: getBroadcastAddress: SocketUtils.getLocalNetworkInterfaceInfo returns ni=null, check network connection");
			return null;
		}
		List<InterfaceAddress> ialist = ni.getInterfaceAddresses();
		for (InterfaceAddress ia : ialist) {
			try {
				String hostAddress = ia.getBroadcast().getHostAddress();
				if (hostAddress != null && !hostAddress.isEmpty()) {
					//System.out.println("hostAddress='" + hostAddress + "' MTU: " + ni.getMTU() );
					return hostAddress;
				}
			} catch (Exception ex) {}
		}
		return null;
    }
}
