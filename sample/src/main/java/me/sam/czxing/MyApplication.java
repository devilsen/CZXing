package me.sam.czxing;

import android.app.Application;
import android.content.Context;

/**
 * desc :
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class MyApplication extends Application {

    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
