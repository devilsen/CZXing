package me.devilsen.czxing.thread;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorUtil {

    private static Executor sMainExecutor;
    private static Handler sMainHandler;
    private static Executor sBackgroundExecutor;
    private static Executor sIOExecutor;


    private synchronized static Executor getMainExecutor() {
        if (sMainExecutor == null) {
            sMainHandler = new Handler(Looper.getMainLooper());
            sMainExecutor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    sMainHandler.post(command);
                }
            };
        }
        return sMainExecutor;
    }

    public synchronized static Executor getIOExecutor() {
        if (sIOExecutor == null) {
            int processors = Runtime.getRuntime().availableProcessors() * 2;
            sIOExecutor = new ThreadPoolExecutor(processors, processors, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new DefaultThreadFactory());
            ((ThreadPoolExecutor) sIOExecutor).allowCoreThreadTimeOut(true);
        }
        return sIOExecutor;
    }

    public synchronized static Executor getBackgroundExecutor() {
        if (sBackgroundExecutor == null) {
            int processors = Runtime.getRuntime().availableProcessors();
            sBackgroundExecutor = new ThreadPoolExecutor(processors, processors, 5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                    new DefaultThreadFactory());
            ((ThreadPoolExecutor) sBackgroundExecutor).allowCoreThreadTimeOut(true);
        }
        return sBackgroundExecutor;
    }

    public static void runOnUiThread(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            getMainExecutor().execute(runnable);
        } else {
            runnable.run();
        }
    }

    public static void runInBackground(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        getBackgroundExecutor().execute(runnable);
    }

    public static ThreadFactory threadFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread result = new Thread(runnable, name);
                result.setDaemon(daemon);
                return result;
            }
        };
    }

    /**
     * The default thread factory.
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "CZXingPool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }


}
