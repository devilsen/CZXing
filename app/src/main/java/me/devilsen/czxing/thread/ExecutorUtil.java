package me.devilsen.czxing.thread;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class ExecutorUtil {
    private static Executor sMainExecutor;
    private static Handler sMainHandler;

    private synchronized static Executor getMainExecutor() {
        if (sMainExecutor == null) {
            sMainHandler = new Handler(Looper.getMainLooper());
            sMainExecutor = new Executor() {
                @Override
                public void execute(@NonNull Runnable command) {
                    sMainHandler.post(command);
                }
            };
        }
        return sMainExecutor;
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
}
