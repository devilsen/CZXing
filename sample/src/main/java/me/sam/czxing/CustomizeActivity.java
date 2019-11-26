package me.sam.czxing;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.compat.ActivityCompat;
import me.devilsen.czxing.compat.ContextCompat;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;
import me.devilsen.czxing.util.ScreenUtil;
import me.devilsen.czxing.util.SoundPoolUtil;
import me.devilsen.czxing.view.ScanBoxView;
import me.devilsen.czxing.view.ScanListener;
import me.devilsen.czxing.view.ScanView;

/**
 * desc : 自定义扫码界面
 * date : 2019-11-18
 *
 * @author : dongSen
 */
public class CustomizeActivity extends AppCompatActivity implements View.OnClickListener,
        ScanListener, ScanListener.AnalysisBrightnessListener {

    private static final int PERMISSIONS_REQUEST_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_STORAGE = 2;
    private static final int CODE_SELECT_IMAGE = 100;

    private ScanView mScanView;
    private SoundPoolUtil mSoundPoolUtil;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customize_scan);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.hide();
        }
        ScreenUtil.setFullScreen(this);

        LinearLayout titleLayout = findViewById(R.id.layout_customize_scan_title);
        ImageView backImg = findViewById(R.id.image_customize_scan_back);
        TextView albumTxt = findViewById(R.id.text_view_customize_scan_album);
        mScanView = findViewById(R.id.surface_customize_view_scan);
        TextView myCodeTxt = findViewById(R.id.text_view_customize_my_qr_code);
        TextView option1Txt = findViewById(R.id.text_view_customize_option_1);
        TextView option2Txt = findViewById(R.id.text_view_customize_option_2);
        TextView option3Txt = findViewById(R.id.text_view_customize_option_3);

        // 设置扫描模式
//        mScanView.setScanMode(ScanView.SCAN_MODE_MIX);
        // 设置扫描格式 BarcodeFormat
//        mScanView.setBarcodeFormat();

        ScanBoxView scanBox = mScanView.getScanBox();
        // 设置扫码框上下偏移量，可以为负数
        scanBox.setBoxTopOffset(-BarCodeUtil.dp2px(this, 100));
        // 设置扫码框四周的颜色
        scanBox.setMaskColor(Color.parseColor("#9C272626"));
        // 设定四个角的颜色
//        scanBox.setCornerColor();
        // 设定扫描框的边框颜色
//        scanBox.setBorderColor();
        // 设置边框长度(扫码框大小)
//        scanBox.setBorderSize();
        // 设定扫描线的颜色
//        scanBox.setScanLineColor();
        // 设置手电筒打开时的图标
//        scanBox.setFlashLightOnDrawable();
        // 设置手电筒关闭时的图标
//        scanBox.setFlashLightOffDrawable();
        // 设置闪光灯打开时的提示文字
//        scanBox.setFlashLightOnText();
        // 设置闪光灯关闭时的提示文字
//        scanBox.setFlashLightOffText();
        // 不使用手电筒图标及提示
//        scanBox.invisibleFlashLightIcon();
        // 设置扫码框下方的提示文字
//        scanBox.setScanNoticeText();

        backImg.setOnClickListener(this);
        albumTxt.setOnClickListener(this);
        // 获取扫码回调
        mScanView.setScanListener(this);
        // 获取亮度测量结果
        mScanView.setAnalysisBrightnessListener(this);
        myCodeTxt.setOnClickListener(this);
        option1Txt.setOnClickListener(this);
        option2Txt.setOnClickListener(this);
        option3Txt.setOnClickListener(this);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) titleLayout.getLayoutParams();
        layoutParams.topMargin = ScreenUtil.getStatusBarHeight(this);

        mSoundPoolUtil = new SoundPoolUtil();
        mSoundPoolUtil.loadDefault(this);

        requestCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScanView.openCamera(); // 打开后置摄像头开始预览，但是并未开始识别
        mScanView.startScan();  // 显示扫描框，并开始识别
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScanView.stopScan();
        mScanView.closeCamera(); // 关闭摄像头预览，并且隐藏扫描框
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
        switch (id) {
            case R.id.image_customize_scan_back:
                finish();
                break;
            case R.id.text_view_customize_scan_album:
                requestStoragePermission();
                break;
            case R.id.text_view_customize_my_qr_code:
                startActivity(new Intent(this, MyCardActivity.class));
                break;
            case R.id.text_view_customize_option_1:
                Toast.makeText(this, "option 1", Toast.LENGTH_SHORT).show();
                break;
            case R.id.text_view_customize_option_2:
                Toast.makeText(this, "option 2", Toast.LENGTH_SHORT).show();
                break;
            case R.id.text_view_customize_option_3:
                Toast.makeText(this, "option 3", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CODE_SELECT_IMAGE) {
            decodeImage(data);
            finish();
        }
    }

    @Override
    public void onScanSuccess(String result, BarcodeFormat format) {
        mSoundPoolUtil.play();

        showResult(result);
        finish();
    }

    /**
     * 可以通过此回调来控制自定义的手电筒显隐藏
     *
     * @param isDark 是否处于黑暗的环境
     */
    @Override
    public void onAnalysisBrightness(boolean isDark) {
        if (isDark) {
            Log.d("analysisBrightness", "您处于黑暗的环境，建议打开手电筒");
        } else {
            Log.d("analysisBrightness", "正常环境，如果您打开了手电筒，可以关闭");
        }
    }

    @Override
    public void onOpenCameraError() {
        Log.e("onOpenCameraError", "onOpenCameraError");
    }

    public void showResult(String result) {
        Intent intent = new Intent(this, DelegateActivity.class);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    private void decodeImage(Intent intent) {
        Uri selectImageUri = intent.getData();
        if (selectImageUri == null) {
            return;
        }
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(selectImageUri, filePathColumn, null, null, null);
        if (cursor == null) {
            return;
        }
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();

        Bitmap bitmap = BitmapUtil.getDecodeAbleBitmap(picturePath);
        if (bitmap == null) {
            return;
        }

        CodeResult result = BarcodeReader.getInstance().read(bitmap);
        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        } else {
            Log.e("Scan >>> ", result.getText());
        }
        showResult(result.getText());
    }

    /**
     * 获取摄像头权限（实际测试中，使用第三方获取权限工具，可能造成摄像头打开失败）
     */
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        }
    }

    private void requestStoragePermission() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_STORAGE);
        } else {
            Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(albumIntent, CODE_SELECT_IMAGE);
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
        } else if (requestCode == PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(albumIntent, CODE_SELECT_IMAGE);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
