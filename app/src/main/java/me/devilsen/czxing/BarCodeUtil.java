package me.devilsen.czxing;

import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

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

    public static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

}
