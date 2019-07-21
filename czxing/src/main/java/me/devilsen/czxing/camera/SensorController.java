package me.devilsen.czxing.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * 加速度控制器  用来控制对焦
 *
 * @author jerry
 * https://github.com/WellerV/SweetCamera
 */
public class SensorController implements SensorEventListener {

    private static final String TAG = "SensorController";
    private static final int DELAY_DURATION = 500;

    private static final int STATUS_NONE = 0;
    private static final int STATUS_STATIC = 1;
    private static final int STATUS_MOVE = 2;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private int mX, mY, mZ;
    private long lastStaticStamp = 0;

    private boolean isFocusing = false;
    private boolean canFocusIn = false;  //内部是否能够对焦控制机制
    private boolean canFocus = false;

    private int statue = STATUS_NONE;

    private int focusing = 1;           // 1 表示没有被锁定 0表示被锁定
    private CameraFocusListener mCameraFocusListener;

    SensorController(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);// TYPE_GRAVITY
    }

    public void setCameraFocusListener(CameraFocusListener mCameraFocusListener) {
        this.mCameraFocusListener = mCameraFocusListener;
    }

    public void onStart() {
        restParams();
        canFocus = true;
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void onStop() {
        mSensorManager.unregisterListener(this, mSensor);
        canFocus = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == null) {
            return;
        }

        if (isFocusing) {
            restParams();
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastStaticStamp < 300) {
            return;
        }
        lastStaticStamp = now;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            int x = (int) event.values[0];
            int y = (int) event.values[1];
            int z = (int) event.values[2];

            int px = Math.abs(mX - x);
            int py = Math.abs(mY - y);
            int pz = Math.abs(mZ - z);
            double value = Math.sqrt(px * px + py * py + pz * pz);
            if (value > 1.4) {
//                Log.i(TAG, "mobile moving");
                statue = STATUS_MOVE;
            } else {
//                Log.i(TAG, "mobile static");
                if (statue == STATUS_MOVE) {
                    if (mCameraFocusListener != null) {
//                        Log.i(TAG, "mobile static callback");
                        mCameraFocusListener.onFrozen();
                    }
                }
                statue = STATUS_STATIC;
            }

            mX = x;
            mY = y;
            mZ = z;
        }
    }

    private void restParams() {
        statue = STATUS_NONE;
        canFocusIn = false;
        mX = 0;
        mY = 0;
        mZ = 0;
    }

    /**
     * 对焦是否被锁定
     */
    public boolean isFocusLocked() {
        if (canFocus) {
            return focusing <= 0;
        }
        return false;
    }

    /**
     * 锁定对焦
     */
    public void lockFocus() {
        isFocusing = true;
        focusing--;
        Log.i(TAG, "lockFocus");
    }

    /**
     * 解锁对焦
     */
    public void unlockFocus() {
        isFocusing = false;
        focusing++;
        Log.i(TAG, "unlockFocus");
    }

    public void restFoucs() {
        focusing = 1;
    }

    public interface CameraFocusListener {
        void onFrozen();
    }
}
