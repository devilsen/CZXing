package me.devilsen.czxing;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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

    public static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }

    private static long time = 0;

    public static void printTime() {
        long now = System.currentTimeMillis();
        Log.e("time:", (now - time) + "");
        time = now;
    }

    public static void copyAssets(Context context, String path) {
        File model = new File(path);
        File file = new File(Environment.getExternalStorageDirectory(), model.getName());
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            InputStream is = context.getAssets().open(path);
            int len;
            byte[] b = new byte[2048];
            while ((len = is.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            fos.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
