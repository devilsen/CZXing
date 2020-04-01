package me.sam.czxing;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.BuildConfig;
import me.devilsen.czxing.Scanner;
import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;
import me.devilsen.czxing.view.ScanActivityDelegate;
import me.devilsen.czxing.view.ScanView;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_SELECT_IMAGE = 1;
    private TextView resultTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BarCodeUtil.setDebug(BuildConfig.DEBUG);

        resultTxt = findViewById(R.id.text_view_result);
        requestPermission();
    }

    public void scan(View view) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_select_from_album_1);
        CodeResult result = BarcodeReader.getInstance().read(bitmap);

        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        } else {
            Log.e("Scan >>> ", result.getText());
        }

        resultTxt.setText(result.getText());
    }

    public void write(View view) {
        Intent intent = new Intent(this, WriteCodeActivity.class);
        startActivity(intent);
    }

    public void writeQrCode(View view) {
        Intent intent = new Intent(this, WriteQRCodeActivity.class);
        startActivity(intent);
    }

    /**
     * 内置API展示
     */
    public void openScan(View view) {
        Resources resources = getResources();
        List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));

        Scanner.with(this)
                .setMaskColor(resources.getColor(R.color.mask_color))
                .setBorderColor(resources.getColor(R.color.box_line))
                .setBorderSize(BarCodeUtil.dp2px(this, 200))
//                .setBorderSize(BarCodeUtil.dp2px(this, 200), BarCodeUtil.dp2px(this, 100))
                .setCornerColor(resources.getColor(R.color.corner))
                .setScanLineColors(scanColors)
//                .setHorizontalScanLine()
                .setScanMode(ScanView.SCAN_MODE_BIG)
//                .setBarcodeFormat(BarcodeFormat.EAN_13)
                .setTitle("我的扫一扫")
                .showAlbum(true)
                .setScanNoticeText("扫描二维码")
                .setFlashLightOnText("打开闪光灯")
                .setFlashLightOffText("关闭闪光灯")
//                .setFlashLightInvisible()
                .setFlashLightOnDrawable(R.drawable.ic_highlight_blue_open_24dp)
                .setFlashLightOffDrawable(R.drawable.ic_highlight_white_close_24dp)
                .continuousScan()
                .setOnClickAlbumDelegate(new ScanActivityDelegate.OnClickAlbumDelegate() {
                    @Override
                    public void onClickAlbum(Activity activity) {
                        Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        activity.startActivityForResult(albumIntent, CODE_SELECT_IMAGE);
                    }

                    @Override
                    public void onSelectData(int requestCode, Intent data) {
                        if (requestCode == CODE_SELECT_IMAGE) {
                            decodeImage(data);
                        }
                    }
                })
                .setOnScanResultDelegate(new ScanActivityDelegate.OnScanDelegate() {
                    @Override
                    public void onScanResult(@NonNull final Activity activity, @NonNull final String result, @NonNull BarcodeFormat format) {
                        // 如果有回调，则必然有值,因为要避免AndroidX和support包的差异，所以没有默认的注解
//                        Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
//                        intent.putExtra("result", result);
//                        startActivity(intent);

                        final String showContent = "format: " + format.name() + "  code: " + result;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if ("123".equals(result)) {
                                    activity.finish();
                                }
                                Toast.makeText(MainActivity.this, showContent, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .start();
    }

    public void openScanBox(View view) {
        Intent intent = new Intent(this, ScanBoxTestActivity.class);
        startActivity(intent);
    }

    /**
     * 测试复杂二维码
     */
    public void test() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test_boder_complex_8);
        CodeResult result = BarcodeReader.getInstance().read(bitmap);

        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        } else {
            Log.e("Scan >>> ", result.getText());
        }

        resultTxt.setText(result.getText());
    }

    public void openCustomizeActivity(View view) {
        Intent intent = new Intent(this, CustomizeActivity.class);
        startActivity(intent);
    }

    private void requestPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.CAMERA, Permission.Group.STORAGE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {

                    }
                })
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {

                    }
                })
                .start();
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

        // 适当压缩图片
        Bitmap bitmap = BitmapUtil.getDecodeAbleBitmap(picturePath);
        // 这个方法比较耗时，推荐放到子线程执行
        CodeResult result = BarcodeReader.getInstance().read(bitmap);
        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        } else {
            Log.e("Scan >>> ", result.getText());
        }
        resultTxt.setText(result.getText());
    }

}
