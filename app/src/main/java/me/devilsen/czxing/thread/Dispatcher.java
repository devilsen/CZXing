package me.devilsen.czxing.thread;

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

    private ExecutorService executorService;

    public Dispatcher() {
        executorService = new ThreadPoolExecutor(2,
                2,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }

    synchronized void execute(Runnable runnable) {
        executorService.execute(runnable);
    }

}
