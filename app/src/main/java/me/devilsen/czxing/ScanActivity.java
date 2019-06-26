package me.devilsen.czxing;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import me.devilsen.czxing.processor.CameraDataProcessor;


/**
 * desc : 扫码页面
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class ScanActivity extends AppCompatActivity implements
        Camera.PreviewCallback, CameraHelper.OnChangedSizeListener {

    private static final String TAG = "Scan >>> ";
    private SurfaceView surfaceView;
    private CameraDataProcessor cameraDataProcessor;
    private CameraHelper cameraHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        surfaceView = findViewById(R.id.surface_view_scan);

//        Point point = new Point();
//        getWindowManager().getDefaultDisplay().getSize(point);
//        int screenWidth = point.x;
//        int screenHeight = point.y;

        requestPermission();

        cameraDataProcessor = new CameraDataProcessor();

        surfaceView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                surfaceView.removeOnLayoutChangeListener(this);
                initCamera(view.getWidth(), view.getHeight());
            }
        });

    }

    private void initCamera(int width, int height) {
        Log.e(TAG, "surface: " + width + " " + height);

        cameraHelper = new CameraHelper(this,
                Camera.CameraInfo.CAMERA_FACING_BACK,
                width,
                height);

        cameraHelper.setPreviewDisplay(surfaceView.getHolder());
        cameraHelper.setPreviewCallback(this);
        cameraHelper.setOnChangedSizeListener(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        String result = cameraDataProcessor.process(data, camera);
        if (result != null) {
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChanged(int w, int h) {

    }

    private void requestPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA)
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                })
                .onDenied(permissions -> {
                    // Storage permission are not allowed.
                })
                .start();
    }


    public void start(View view) {
        cameraDataProcessor.openSwitch();
    }

    public void stop(View view) {
        cameraDataProcessor.closeSwitch();
    }

    public void switchCamera(View view) {
        cameraHelper.switchCamera();
    }
}
