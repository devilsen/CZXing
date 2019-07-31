package me.devilsen.czxing;

import android.content.Context;

/**
 * desc :
 * date : 2019/07/31
 *
 * @author : dongsen
 */
public class Scanner {

    public static ScannerManager with(Context context) {
        return new ScannerManager(context);
    }

}
