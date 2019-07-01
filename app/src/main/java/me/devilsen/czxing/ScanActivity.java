package me.devilsen.czxing;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import me.devilsen.czxing.view.ScanListener;
import me.devilsen.czxing.view.ScanView;


/**
 * desc : 扫码页面
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class ScanActivity extends AppCompatActivity implements ScanListener {

    private static final String TAG = "Scan >>> ";

    private ScanView mScanView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        BarCodeUtil.setDebug(true);

        mScanView = findViewById(R.id.surface_view_scan);
        mScanView.setScanListener(this);
        requestPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mScanView.openCamera(); // 打开后置摄像头开始预览，但是并未开始识别
//        mScanView.startScan();  // 显示扫描框，并开始识别
    }

    @Override
    protected void onStop() {
        mScanView.closeCamera(); // 关闭摄像头预览，并且隐藏扫描框
        mScanView.stopScan();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mScanView.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    @Override
    public void onCameraOpen() {
        mScanView.startScan();  // 显示扫描框，并开始识别
    }

    @Override
    public void onScanSuccess(String result) {

    }

    @Override
    public void onBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onOpenCameraError() {

    }


    private void requestPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.STORAGE)
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                })
                .onDenied(permissions -> {
                    // Storage permission are not allowed.
                })
                .start();
    }

}
