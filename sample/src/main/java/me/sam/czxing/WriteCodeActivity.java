package me.sam.czxing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

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
public class WriteCodeActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView qrcodeImage;
    private ImageView qrcodeLogoImage;
    private ImageView barcodeImage;
    private ImageView barcodeColorImage;
    private BarcodeWriter writer;
    private BarcodeReader reader;
    private Bitmap barcodeBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_code);

        qrcodeImage = findViewById(R.id.image_view_qr_code_1);
        qrcodeLogoImage = findViewById(R.id.image_view_qr_code_2);
        barcodeImage = findViewById(R.id.image_view_bar_code_1);
        barcodeColorImage = findViewById(R.id.image_view_bar_code_2);

        writer = new BarcodeWriter();
        reader = new BarcodeReader(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.AZTEC,
                BarcodeFormat.CODABAR,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.EAN_8,
                BarcodeFormat.EAN_13,
                BarcodeFormat.ITF,
                BarcodeFormat.MAXICODE,
                BarcodeFormat.PDF_417,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.UPC_EAN_EXTENSION
        );

//        writeQrCode();
        writeBarCode();

        barcodeImage.setOnClickListener(this);
    }

    private void writeQrCode() {
        Bitmap bitmap1 = writer.write("Hello World",
                BarCodeUtil.dp2px(this, 150),
                Color.BLACK);

        Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_avatar);

        Bitmap bitmap2 = writer.write("你好，世界",
                BarCodeUtil.dp2px(this, 150),
                Color.parseColor("#2196F3"),
                logoBitmap);

        if (bitmap1 != null) {
            qrcodeImage.setImageBitmap(bitmap1);
        }

        if (bitmap2 != null) {
            qrcodeLogoImage.setImageBitmap(bitmap2);
        }
    }

    private void writeBarCode() {
        barcodeBitmap = writer.writeBarCode("Hello CZXing",
                BarCodeUtil.dp2px(this, 150), BarCodeUtil.dp2px(this, 80));

        Bitmap bitmap2 = writer.writeBarCode("20190729",
                BarCodeUtil.dp2px(this, 150), BarCodeUtil.dp2px(this, 80),
                Color.parseColor("#2196F3"));

        if (barcodeBitmap != null) {
            barcodeImage.setImageBitmap(barcodeBitmap);
        }

        if (bitmap2 != null) {
            barcodeColorImage.setImageBitmap(bitmap2);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.image_view_bar_code_1) {
            read(barcodeBitmap);
        }
    }

    public void read(Bitmap bitmap) {
        BarcodeReader.Result result = reader.read(bitmap, bitmap.getWidth(), bitmap.getHeight());
        if (result != null) {
            Log.d("read code", result.getText() + " format " + result.getFormat());
            Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
        }
    }
}
