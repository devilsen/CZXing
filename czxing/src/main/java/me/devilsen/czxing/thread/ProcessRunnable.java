package me.devilsen.czxing.thread;

import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.processor.BarcodeProcessor;

/**
 * desc : 二维码处理线程
 * date : 2019-07-01 18:47
 *
 * @author : dongSen
 */
public class ProcessRunnable implements Runnable {

    private Dispatcher dispatcher;
    private FrameData frameData;
    private BarcodeProcessor mBarcodeProcessor;
    private Callback mDecodeCallback;

    ProcessRunnable(Dispatcher dispatcher, FrameData frameData, Callback callback) {
        this.dispatcher = dispatcher;
        this.frameData = frameData;
        this.mDecodeCallback = callback;
        mBarcodeProcessor = new BarcodeProcessor();
    }

    @Override
    public void run() {
        try {
            if (frameData.left != 0 && frameData.top != 0) {
                boolean isDark = mBarcodeProcessor.analysisBrightness(frameData.data, frameData.width, frameData.height);
                if (mDecodeCallback != null) {
                    mDecodeCallback.onDarkBrightness(isDark);
                }
                if (isDark){
                    return;
                }
            }

            long start = System.currentTimeMillis();
            CodeResult result = mBarcodeProcessor.processBytes(frameData.data,
                    frameData.left,
                    frameData.top,
                    frameData.width,
                    frameData.height,
                    frameData.rowWidth,
                    frameData.rowHeight);

            BarCodeUtil.d("reader time: " + (System.currentTimeMillis() - start));

            if (result != null && mDecodeCallback != null) {
                mDecodeCallback.onDecodeComplete(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dispatcher.finished(this);
        }
    }

    void cancel() {
        mBarcodeProcessor.cancel();
    }

    public int enqueue() {
        return dispatcher.enqueue(this);
    }
}
