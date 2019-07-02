package me.devilsen.czxing.thread;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
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

    private static final int MAX_RUNNABLE = 30;
    private ExecutorService executorService;

    private final Deque<ProcessRunnable> readyAsyncCalls = new ArrayDeque<>();

    public Dispatcher() {
        executorService = new ThreadPoolExecutor(2,
                2,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    public ProcessRunnable newRunnable(FrameData frameData, Callback callback) {
        return new ProcessRunnable(this, frameData, callback);
    }

    public ProcessRunnable newRunnable(byte[] data, int left, int top, int width, int height, int rowWidth, Callback callback) {
        return newRunnable(new FrameData(data, left, top, width, height, rowWidth), callback);
    }

    public synchronized void enqueue(ProcessRunnable runnable) {
        readyAsyncCalls.addFirst(runnable);
        if (readyAsyncCalls.size() > MAX_RUNNABLE) {
            readyAsyncCalls.removeLast();
        }
        execute(runnable);
        Log.e(TAG, "async size " + readyAsyncCalls.size());
    }

    private synchronized void execute(ProcessRunnable runnable) {
        executorService.execute(runnable);
    }

    public void finished(ProcessRunnable runnable) {
        finish(readyAsyncCalls, runnable);
    }

    private void finish(Deque<ProcessRunnable> readyAsyncCalls, ProcessRunnable runnable) {
        synchronized (this) {
            if (readyAsyncCalls.size() > 0) {
                readyAsyncCalls.remove(runnable);
                promoteCalls();
            }
        }
    }

    private void promoteCalls() {
        if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.

        ProcessRunnable first = readyAsyncCalls.getFirst();
        execute(first);
    }

    public synchronized void cancelAll() {
        for (ProcessRunnable runnable : readyAsyncCalls) {
            runnable.cancel();
        }
        readyAsyncCalls.clear();
    }

}
