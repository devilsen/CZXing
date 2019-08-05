package me.devilsen.czxing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import me.devilsen.czxing.compat.ActivityCompat;
import me.devilsen.czxing.compat.ContextCompat;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ScreenUtil;
import me.devilsen.czxing.util.SoundPoolUtil;
import me.devilsen.czxing.view.ScanActivityDelegate;
import me.devilsen.czxing.view.ScanBoxView;
import me.devilsen.czxing.view.ScanListener;
import me.devilsen.czxing.view.ScanView;


/**
 * desc : 扫码页面
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class ScanActivity extends Activity implements ScanListener, View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private ScanView mScanView;
    private ScanActivityDelegate.OnScanDelegate scanDelegate;
    private ScanActivityDelegate.OnClickAlbumDelegate clickAlbumDelegate;
    private SoundPoolUtil mSoundPoolUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        BarCodeUtil.setDebug(BuildConfig.DEBUG);
        ScreenUtil.setFullScreen(this);

        LinearLayout titleLayout = findViewById(R.id.layout_scan_title);
        ImageView backImg = findViewById(R.id.image_scan_back);
        TextView album = findViewById(R.id.text_view_scan_album);
        mScanView = findViewById(R.id.surface_view_scan);

        backImg.setOnClickListener(this);
        album.setOnClickListener(this);
        mScanView.setScanListener(this);

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) titleLayout.getLayoutParams();
        layoutParams.topMargin = ScreenUtil.getStatusBarHeight(this);

        scanDelegate = ScanActivityDelegate.getInstance().getScanDelegate();
        clickAlbumDelegate = ScanActivityDelegate.getInstance().getOnClickAlbumDelegate();

        mSoundPoolUtil = new SoundPoolUtil();
        mSoundPoolUtil.loadDefault(this);

        initData();
        requestPermission();
    }

    private void initData() {
        ScannerManager.ScanOption option = getIntent().getParcelableExtra("option");
        if (option == null) {
            return;
        }
        ScanBoxView scanBox = mScanView.getScanBox();
        scanBox.setCornerColor(option.getCornerColor());
        scanBox.setBorderColor(option.getBorderColor());
        scanBox.setScanLineColor(option.getScanLineColors());
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
//        mScanView.startScan();  // 显示扫描框，并开始识别
//        mScanView.resetZoom();  // 重置相机扩大倍数
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
        mSoundPoolUtil.release();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.image_scan_back) {
            finish();
        } else if (id == R.id.text_view_scan_album) {
            if (clickAlbumDelegate != null) {
                clickAlbumDelegate.onClickAlbum(this);
            }
        }
    }

    @Override
    public void onScanSuccess(String result) {
        mSoundPoolUtil.play();

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

    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScanView.openCamera();
                mScanView.startScan();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && clickAlbumDelegate != null) {
            clickAlbumDelegate.onSelectData(requestCode, data);
            finish();
        }
    }
}
