package me.devilsen.czxing.thread;

import me.devilsen.czxing.BarcodeReader;

/**
 * desc :
 * date : 2019-07-02 20:39
 *
 * @author : dongSen
 */
public interface Callback {

    void onDecodeComplete(BarcodeReader.Result result);

    void onDarkBrightness(boolean isDark);

}
