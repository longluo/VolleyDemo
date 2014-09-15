package com.longluo.volleydemo.util;

import android.util.Log;

public class iQiyiInterface {
    private static final String TAG = "iQiyiInterface";

    public static final int secureCode1 = 1771777171;
    public static final String secureCode2 = "L8d:d^)DBei";

    public static final String HOME_URL = "http://iface2.iqiyi.com/php/xyz/entry/galaxy.php";

    public static final String PLATFORM = "GPhone_trd_oppo";
    public static final String oemOppoKey = "10020202ddf238a3ed4b7fbac0e1c989";
    public static final String UA_VERSION = "5.4";

    /*
     * public static String encryptAndUncrypt(String value, char secret) {
     * byte[] bt = value.getBytes(); for (int i = 0; i < bt.length; i++) { bt[i]
     * = (byte) (bt[i] ^ (int)secret); }
     * 
     * return new String(bt, 0, bt.length); }
     */

    public static String getURL() {
        return HOME_URL;
    }

    public static String getIMEI() {

        return null;
    }

    public static String getTimestamp() {
        long time = System.currentTimeMillis();
        int seconds = (int) (time / 1000);

        Log.d(TAG, "time=" + time + " seconds=" + seconds);

        String timestamp = String.valueOf(seconds);

        return timestamp;
    }

    public static String getEncryptTimestamp() {
        long time = System.currentTimeMillis();
        int seconds = (int) (time / 1000);

        int test = seconds ^ secureCode1;

        Log.d(TAG, "seconds=" + seconds + ",test=" + test);

        return String.valueOf(test);
    }

    public static String getSign() {
        StringBuilder signStr = new StringBuilder();

        signStr.append(getTimestamp());
        signStr.append(secureCode2);
        signStr.append(oemOppoKey);
        signStr.append(UA_VERSION);

        return MD5.GetMD5Code(signStr.toString(), true);
    }

}
