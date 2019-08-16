package me.devilsen.czxing.thread;

import me.devilsen.czxing.code.CodeResult;

/**
 * desc :
 * date : 2019-07-02 20:39
 *
 * @author : dongSen
 */
public interface Callback {

    void onDecodeComplete(CodeResult result);

    void onDarkBrightness(boolean isDark);

}
