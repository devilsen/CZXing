package me.devilsen.czxing;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView resultTxt;
    private BarcodeReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTxt = findViewById(R.id.text_view_result);

        reader = new BarcodeReader(BarcodeFormat.QR_CODE);
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
}
