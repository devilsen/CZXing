package me.devilsen.czxing.processor;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * desc : 处理器
 * date : 2019-06-25
 *
 * @author : dongSen
 */
abstract class Processor implements LifecycleObserver {

    boolean mSwitch;

    public void openSwitch() {
        mSwitch = true;
    }

    public void closeSwitch() {
        mSwitch = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void start() {
        mSwitch = true;
    }

    abstract void onStart();

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void stop() {
        mSwitch = false;
    }

    abstract void onStop();

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    abstract void onDestroy();
}
