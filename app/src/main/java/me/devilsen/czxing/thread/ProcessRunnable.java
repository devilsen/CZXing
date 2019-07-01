package me.devilsen.czxing.thread;

import me.devilsen.czxing.processor.BarcodeProcessor;

/**
 * desc :
 * date : 2019-07-01 18:47
 *
 * @author : dongSen
 */
public class ProcessRunnable implements Runnable {

    private Dispatcher dispatcher;
    private FrameData frameData;
    private BarcodeProcessor mBarcodeProcessor;

    ProcessRunnable(Dispatcher dispatcher, FrameData frameData) {
        this.dispatcher = dispatcher;
        this.frameData = frameData;
        mBarcodeProcessor = new BarcodeProcessor();
    }

    @Override
    public void run() {
        try {
            mBarcodeProcessor.processBytes(frameData.data,
                    frameData.left,
                    frameData.top,
                    frameData.width,
                    frameData.height,
                    frameData.rowWidth);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dispatcher.finished(this);
        }
    }

    public void cancel() {
        mBarcodeProcessor.cancel();
    }

    public void enqueue() {
        dispatcher.enqueue(this);
    }
}
