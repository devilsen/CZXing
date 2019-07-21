package me.sam.czxing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;
import me.devilsen.czxing.ScanActivity;
import me.devilsen.czxing.view.ScanActivityDelegate;

public class MainActivity extends AppCompatActivity {

    private TextView resultTxt;
    private BarcodeReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTxt = findViewById(R.id.text_view_result);

        reader = new BarcodeReader(BarcodeFormat.QR_CODE);

        requestPermission();

        ScanActivityDelegate.getInstance().setScanResultDelegate(new ScanActivityDelegate.OnScanDelegate() {
            @Override
            public void onScanResult(String result) {
                Intent intent = new Intent(MainActivity.this, DelegateActivity.class);
                intent.putExtra("result", result);
                startActivity(intent);
            }

            @Override
            public void onClickCard() {
                Intent intent = new Intent(MainActivity.this, MyCardActivity.class);
                startActivity(intent);
            }
        });
    }

    public void scan(View view) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test1);
        BarcodeReader.Result result = reader.read(bitmap, bitmap.getWidth(), bitmap.getHeight());

        if (result == null) {
            Log.e("Scan >>> ", "no code");
            return;
        }
        resultTxt.setText(result.getText());
    }

    public void openScan(View view) {
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
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
