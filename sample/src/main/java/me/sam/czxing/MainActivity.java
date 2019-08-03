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

import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;
import me.devilsen.czxing.Scanner;
import me.devilsen.czxing.util.BitmapUtil;
import me.devilsen.czxing.util.SaveImageUtil;
import me.devilsen.czxing.view.ScanActivityDelegate;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_SELECT_IMAGE = 1;
    private TextView resultTxt;
    private BarcodeReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTxt = findViewById(R.id.text_view_result);
        reader = new BarcodeReader(BarcodeFormat.QR_CODE);
        requestPermission();
    }

    public void scan(View view) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);
        BarcodeReader.Result result = reader.read(bitmap);

        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        }
        resultTxt.setText(result.getText());
    }

    public void write(View view) {
        Intent intent = new Intent(this, WriteCodeActivity.class);
        startActivity(intent);
    }

    public void openScan(View view) {
//        Intent intent = new Intent(this, ScanActivity.class);
//        startActivity(intent);
        Resources resources = getResources();
        List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));

        Scanner.with(this)
                .setBorderColor(resources.getColor(R.color.box_line))
                .setCornerColor(resources.getColor(R.color.corner))
                .setScanLineColors(scanColors)
                .setOnClickAlbumDelegate(new ScanActivityDelegate.OnClickAlbumDelegate() {
                    @Override
                    public void onClickAlbum(Activity activity) {
                        Intent albumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        activity.startActivityForResult(albumIntent, CODE_SELECT_IMAGE);
                    }

                    @Override
                    public void onSelectData(int requestCode, Intent data) {
                        if (requestCode == CODE_SELECT_IMAGE) {
                            selectPic(data);
                        }
                    }
                })
                .setOnScanResultDelegate(new ScanActivityDelegate.OnScanDelegate() {
                    @Override
                    public void onScanResult(String result) {
                        Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
                        intent.putExtra("result", result);
                        startActivity(intent);
                    }
                })
                .start();
    }

    public void openScanBox(View view) {
        Intent intent = new Intent(this, ScanBoxTestActivity.class);
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

    private void selectPic(Intent intent) {
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

        Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
        if (bitmap == null) {
            return;
        }

        BarcodeReader.Result result = reader.read(bitmap);
        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        }
        resultTxt.setText(result.getText());
    }
}
