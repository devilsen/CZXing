package me.devilsen.czxing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ScreenUtil;
import me.devilsen.czxing.util.SoundPlayer;
import me.devilsen.czxing.view.scanview.ScanActivityDelegate;
import me.devilsen.czxing.view.scanview.ScanBoxView;
import me.devilsen.czxing.view.scanview.ScanLayout;
import me.devilsen.czxing.view.scanview.ScanListener;


/**
 * desc : 扫码页面
 * date : 2019-06-25
 *
 * @author : dongSen
 */
public class ScanActivity extends Activity implements ScanListener, View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int MESSAGE_WHAT_START_SCAN = 10;
    private static final long DELAY_TIME = 800;

    private TextView titleTxt;
    private TextView albumTxt;
    private ScanLayout mScanLayout;
    private SoundPlayer mSoundPoolUtil;

    private boolean isContinuousScan;

    private boolean mSaveInstanceStateFlag;
    private ScanActivityDelegate.OnScanDelegate scanDelegate;
    private ScanActivityDelegate.OnClickAlbumDelegate clickAlbumDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_scan);

        BarCodeUtil.setDebug(BuildConfig.DEBUG);
        ScreenUtil.setFullScreen(this);

        LinearLayout titleLayout = findViewById(R.id.layout_scan_title);
        ImageView backImg = findViewById(R.id.image_scan_back);
        titleTxt = findViewById(R.id.text_view_scan_title);
        albumTxt = findViewById(R.id.text_view_scan_album);
        mScanLayout = findViewById(R.id.surface_view_scan);

        backImg.setOnClickListener(this);
        albumTxt.setOnClickListener(this);
        mScanLayout.setOnScanListener(this);

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) titleLayout.getLayoutParams();
        layoutParams.topMargin = ScreenUtil.getStatusBarHeight(this);

        scanDelegate = ScanActivityDelegate.getInstance().getScanDelegate();
        clickAlbumDelegate = ScanActivityDelegate.getInstance().getOnClickAlbumDelegate();

        mSoundPoolUtil = new SoundPlayer();
        mSoundPoolUtil.loadDefault(this);

        initData();
        requestPermission();
    }

    private void initData() {
        ScannerManager.ScanOption option = getIntent().getParcelableExtra("option");
        if (option == null) {
            return;
        }
        mScanLayout.setBarcodeFormat(option.getBarcodeFormat());
        mScanLayout.setResultColor(option.getResultColor());
        mScanLayout.hideResultColor(option.isHideResultColor());

        ScanBoxView scanBox = mScanLayout.getScanBox();
        scanBox.setScanLineColor(option.getScanLineColors());
        if (option.isScanHorizontal()) {
            scanBox.setHorizontalScanLine();
        }
        mScanLayout.setFlashLightOnDrawable(option.getFlashLightOnDrawable());
        mScanLayout.setFlashLightOffDrawable(option.getFlashLightOffDrawable());
        mScanLayout.setFlashLightOnText(option.getFlashLightOnText());
        mScanLayout.setFlashLightOffText(option.getFlashLightOffText());
        if (option.isDropFlashLight()) {
            mScanLayout.invisibleFlashLightIcon();
        }
        mScanLayout.setScanNoticeText(option.getScanNoticeText());

        String detectorPrototxtPath = option.getDetectPrototxt();
        String detectorCaffeModelPath = option.getDetectCaffeModel();
        String superResolutionPrototxtPath = option.getSuperResolutionPrototxt();
        String superResolutionCaffeModelPath = option.getSuperResolutionCaffeModel();
        mScanLayout.setDetectModel(detectorPrototxtPath, detectorCaffeModelPath, superResolutionPrototxtPath, superResolutionCaffeModelPath);

        // 标题栏
        String title = option.getTitle();
        if (title != null) {
            titleTxt.setText(title);
        }
        // 是否显示相册
        if (option.isShowAlbum()) {
            albumTxt.setVisibility(View.VISIBLE);
        } else {
            albumTxt.setVisibility(View.INVISIBLE);
            albumTxt.setOnClickListener(null);
        }
        // 连续扫描
        isContinuousScan = option.isContinuousScan();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSaveInstanceStateFlag = false;
        mScanLayout.openCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mScanLayout.startDetect();  // 显示扫描框，并开始识别

        if (isContinuousScan) {
            mScanLayout.resetZoom();  // 重置相机扩大倍数
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanLayout.stopDetect();
        mScanLayout.closeCamera(); // 关闭摄像头预览，并且隐藏扫描框
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSaveInstanceStateFlag = true;
    }

    @Override
    protected void onDestroy() {
        mScanLayout.onDestroy(); // 销毁二维码扫描控件
        mSoundPoolUtil.release();
        super.onDestroy();
        // 可能是旋转窗口，不移除代理
        if (mSaveInstanceStateFlag) {
            return;
        }
        ScanActivityDelegate.getInstance().setScanResultDelegate(null);
        ScanActivityDelegate.getInstance().setOnClickAlbumDelegate(null);
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
    public void onScanSuccess(@NonNull List<CodeResult> resultList) {
        mSoundPoolUtil.play();

        int size = resultList.size();
        if (size == 1) {
            CodeResult result = resultList.get(0);
            processOneResult(result.getText(), result.getFormat());
        }
    }

    @Override
    public void onClickResult(CodeResult result) {
        processOneResult(result.getText(), result.getFormat());
    }

    private void processOneResult(String result, BarcodeFormat format) {
        if (scanDelegate != null) {
            scanDelegate.onScanResult(this, result, format);
        } else {
            Intent intent = new Intent(this, ResultActivity.class);
            intent.putExtra("result", result);
            startActivity(intent);
        }
        // 连续扫码，不关闭界面
        if (isContinuousScan) {
            handler.sendEmptyMessageDelayed(MESSAGE_WHAT_START_SCAN, DELAY_TIME);
            return;
        } else {
            mScanLayout.stopPreview();
        }
        finish();
    }

    @Override
    public void onOpenCameraError() {
        Log.e("onOpenCameraError", "onOpenCameraError");
        // 对于一些不可知的问题，比如在一些手机上多次重复打开会出现打开错误，只能用这个方法来规避这个问题
        // 在相机打开失败的情况下，停留在此页面也没有意义
        finish();
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            mScanLayout.startDetect();
            return true;
        }
    });

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
            if (grantResults != null && grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mScanLayout.stopDetect();
                mScanLayout.startDetect();
            } else {
                BarCodeUtil.e("request permission error");
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
