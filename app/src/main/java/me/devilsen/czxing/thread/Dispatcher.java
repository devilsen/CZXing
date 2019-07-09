package me.devilsen.czxing.thread;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
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

    private final Deque<ProcessRunnable> decodeDeque = new ArrayDeque<>();
    private final SynchronousQueue<Runnable> blockingQueue;
    private volatile boolean isRunning;

    public Dispatcher() {
        blockingQueue = new SynchronousQueue<>();
        executorService = new ThreadPoolExecutor(2,
                2,
                10,
                TimeUnit.SECONDS,
                blockingQueue);
    }

    public ProcessRunnable newRunnable(FrameData frameData, Callback callback) {
        return new ProcessRunnable(this, frameData, callback);
    }

    public ProcessRunnable newRunnable(byte[] data, int left, int top, int width, int height, int rowWidth, Callback callback) {
        return newRunnable(new FrameData(data, left, top, width, height, rowWidth), callback);
    }

    public synchronized void enqueue(ProcessRunnable runnable) {
        decodeDeque.addFirst(runnable);
        if (decodeDeque.size() > MAX_RUNNABLE) {
            decodeDeque.removeLast();
        }

        if (!isRunning) {
            execute(runnable);
        }
        Log.e(TAG, "decodeDeque size " + decodeDeque.size() +
                "   blockingQueue: " + blockingQueue.size());
    }

    private synchronized void execute(ProcessRunnable runnable) {
        isRunning = true;
        executorService.execute(runnable);
    }

    public void finished(ProcessRunnable runnable) {
        finish(decodeDeque, runnable);
    }

    private void finish(Deque<ProcessRunnable> decodeDeque, ProcessRunnable runnable) {
        synchronized (this) {
            if (decodeDeque.size() > 0) {
                decodeDeque.remove(runnable);
                promoteCalls();
            }
        }
    }

    private void promoteCalls() {
        if (decodeDeque.isEmpty()) {
            isRunning = false;
            return;
        }

        ProcessRunnable first = decodeDeque.getFirst();
        execute(first);
    }

    public synchronized void cancelAll() {
        for (ProcessRunnable runnable : decodeDeque) {
            runnable.cancel();
        }
        decodeDeque.clear();
        isRunning = false;
    }

}
