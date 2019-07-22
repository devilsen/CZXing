package me.devilsen.czxing;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.thread.ExecutorUtil;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ScreenUtil;
import me.devilsen.czxing.view.ScanActivityDelegate;
import me.devilsen.czxing.view.ScanListener;
import me.devilsen.czxing.view.ScanView;


/**
 * desc : 扫码页面
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class ScanActivity extends AppCompatActivity implements ScanListener {

    private ScanView mScanView;
    private ScanActivityDelegate.OnScanDelegate scanDelegate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        BarCodeUtil.setDebug(false);
        ScreenUtil.setFullScreen(this);

        mScanView = findViewById(R.id.surface_view_scan);
        mScanView.setScanListener(this);
        mScanView.hideCard();

        scanDelegate = ScanActivityDelegate.getInstance().getScanDelegate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mScanView.openCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mScanView.startScan();  // 显示扫描框，并开始识别
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
    public void onScanSuccess(String result) {
        BarCodeUtil.d(result);

        if (scanDelegate != null) {
            scanDelegate.onScanResult(result);
        } else {
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("result", result);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onOpenCameraError() {
        Log.e("onOpenCameraError", "onOpenCameraError");
    }

    @Override
    public void onClickCard() {
        if (scanDelegate != null) {
            scanDelegate.onClickCard();
        }
    }

}
