package me.sam.czxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.BarcodeFormat;
import me.devilsen.czxing.BarcodeReader;
import me.devilsen.czxing.BarcodeWriter;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * desc :
 * date : 2019-07-22 18:25
 *
 * @author : dongSen
 */
public class WriteCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_code);

        ImageView qrcodeImage = findViewById(R.id.image_view_qr_code_1);
        ImageView qrcodeLogoImage = findViewById(R.id.image_view_qr_code_2);

        BarcodeWriter reader = new BarcodeWriter();
        Bitmap bitmap1 = reader.write("Hello World",
                BarCodeUtil.dp2px(this, 200),
                Color.BLACK);

        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Bitmap bitmap2 = reader.write("Hello World",
                BarCodeUtil.dp2px(this, 200),
                Color.parseColor("#2196F3"),
                logoBitmap);

        if (bitmap1 != null) {
            qrcodeImage.setImageBitmap(bitmap1);
        }

        if (bitmap2 != null) {
            qrcodeLogoImage.setImageBitmap(bitmap2);
        }

    }
}
