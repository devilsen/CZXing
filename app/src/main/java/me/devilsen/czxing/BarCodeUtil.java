package me.devilsen.czxing;

import android.util.Log;

/**
 * @author : dongSen
 * date : 2019-06-29 14:01
 * desc :
 */
public class BarCodeUtil {

    private static final String TAG = "CZXing >>> ";

    private static boolean debug;

    public static void setDebug(boolean debug) {
        BarCodeUtil.debug = debug;
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void d(String msg) {
        if (debug) {
            Log.d(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (debug) {
            Log.e(TAG, msg);
        }
    }


}
