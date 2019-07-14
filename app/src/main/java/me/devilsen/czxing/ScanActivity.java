package me.devilsen.czxing;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.thread.ExecutorUtil;
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

        BarCodeUtil.copyAssets(this, "qrcode_cascade.xml");

        BarCodeUtil.setDebug(true);

        mScanView = findViewById(R.id.surface_view_scan);
        mScanView.setScanListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mScanView.openCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mScanView.startScan();  // 显示扫描框，并开始识别
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScanView.onResume();
    }

    @Override
    protected void onStop() {
        mScanView.stopScan();
        mScanView.closeCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mScanView.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    @Override
    public void onCameraOpen() {
//        mScanView.startScan();  // 显示扫描框，并开始识别
    }

    @Override
    public void onScanSuccess(String result) {
        BarCodeUtil.d(result);
        ExecutorUtil.runOnUiThread(() -> Toast.makeText(ScanActivity.this, result, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onOpenCameraError() {
        Log.e("onOpenCameraError", "onOpenCameraError");
    }



}
