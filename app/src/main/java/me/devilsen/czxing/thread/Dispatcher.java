package me.devilsen.czxing.thread;

import android.util.Log;

import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * desc : 任务分发器
 * date : 2019-06-29 11:32
 *
 * @author : dongSen
 */
public final class Dispatcher {

    private static final String TAG = Dispatcher.class.getSimpleName();

    private static final int MAX_RUNNABLE = 10;
    private ExecutorService executorService;

    private final LinkedBlockingDeque<Runnable> blockingDeque;

    public Dispatcher() {
        blockingDeque = new LinkedBlockingDeque<>();
        executorService = new ThreadPoolExecutor(1,
                2,
                10,
                TimeUnit.SECONDS,
                blockingDeque,
                ExecutorUtil.threadFactory("decode dispatcher", false));
    }

    public ProcessRunnable newRunnable(FrameData frameData, Callback callback) {
        return new ProcessRunnable(this, frameData, callback);
    }

    public ProcessRunnable newRunnable(byte[] data, int left, int top, int width, int height, int rowWidth, Callback callback) {
        return newRunnable(new FrameData(data, left, top, width, height, rowWidth), callback);
    }

    synchronized void enqueue(ProcessRunnable runnable) {
        if (blockingDeque.size() > MAX_RUNNABLE) {
            blockingDeque.remove();
        }

        execute(runnable);
        Log.e(TAG, "   blockingDeque: " + blockingDeque.size());
    }

    private synchronized void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

    public void finished(ProcessRunnable runnable) {
        finish(blockingDeque, runnable);
    }

    private void finish(Deque<Runnable> decodeDeque, ProcessRunnable runnable) {
        synchronized (this) {
            if (decodeDeque.size() > 0) {
                decodeDeque.remove(runnable);
                promoteCalls();
            }
        }
    }

    private synchronized void promoteCalls() {
        if (blockingDeque.isEmpty()) {
            return;
        }

        Runnable first = blockingDeque.getFirst();
        execute(first);
    }

    public synchronized void cancelAll() {
        for (Runnable runnable : blockingDeque) {
            ((ProcessRunnable)runnable).cancel();
        }
        blockingDeque.clear();
    }

}
