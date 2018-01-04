package ravn.networkbroadcastshared;

/**
 * Created by bell on 12/24/17.
 */

public class StreamUtils {
    static public void computeCheckSum(byte ba[], int len, byte checksum[]){
        // computes checksum of byte array and places it in first 2 bytes
        // checksum includes all bytes except first 2
        long sum = 0l;
        int padding = (len % 2);
        int i=2;  // start at byte 2
        for (; i<len - padding; i+=2){
            sum += (0xff00 & ( (ba[i] & 0xff) << 8)) + (0xff & ba[i+1]);
        }
        if (padding!=0){
            sum += (0xff00 & ( (ba[i] & 0xff) << 8));
        }
        while ((sum>>16) != 0)
            sum = (sum & 0xFFFF)+(sum >> 16);
        sum = ~sum;
        //System.out.println("computeCheckSum: sum=" + (sum & 0xffff) );
        checksum[1] = (byte)(sum & 0xff);
        checksum[0] = (byte)((sum >>> 8) & 0xff);
    }
    static public void computeCheckSum(byte ba[], byte checksum[]){
        computeCheckSum(ba, ba.length, checksum);
    }
    static public void computeAndWriteCheckSum(byte ba[]){
        // computes checksum of byte array and places it in first 2 bytes
        // checksum includes all bytes except first 2
        byte checksum[] = new byte[2];
        computeCheckSum(ba, checksum);
        ba[0] = checksum[0];
        ba[1] = checksum[1];
    }
    static public int readIntFromByteArrayWithOffset(byte[] barr, int [] off){
        try {
            int ch1 = barr[off[0]++] & 0xFF;
            int ch2 = barr[off[0]++] & 0xFF;
            int ch3 = barr[off[0]++] & 0xFF;
            int ch4 = barr[off[0]++] & 0xFF;
            return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return 0;
    }
    static public String readStringFromByteArrayWithOffset(byte[] ibytes, int [] offset) {
        int len = readIntFromByteArrayWithOffset(ibytes, offset);
        if (len>0){
            String str = new String(ibytes, offset[0], len);
            offset[0] += len;
            return str;
        }
        return "";
    }
    static public void writeIntIntoByteArrayWithOffset(int v, byte[] barr, int [] off){
        barr[off[0]++] = (byte)((v >>> 24) & 0xFF);
        barr[off[0]++] = (byte)((v >>> 16) & 0xFF);
        barr[off[0]++] = (byte)((v >>> 8) & 0xFF);
        barr[off[0]++] = (byte)((v >>> 0) & 0xFF);
    }
    static public void writeStringToByteArrayWithOffset(String str, byte[] obytes, int [] offset) {
        if (str!=null){
            byte[] bytes = str.getBytes();
            writeIntIntoByteArrayWithOffset(bytes.length, obytes, offset);
            System.arraycopy(bytes, 0, obytes, offset[0], bytes.length);
            offset[0] += bytes.length;
        } else {
            writeIntIntoByteArrayWithOffset(0, obytes, offset);
        }
    }
}
