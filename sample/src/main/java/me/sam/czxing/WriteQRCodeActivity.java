package me.sam.czxing;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.BarcodeWriter;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * desc : 生成二维码演示
 * date : 2019-07-22 18:25
 *
 * @author : dongSen
 */
public class WriteQRCodeActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText textEdit;
    private ImageView qrcodeImage;
    private BarcodeWriter writer;
    private BarcodeReader reader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_code_2);

        textEdit = findViewById(R.id.edit_write_qr_code);
        Button writeBtn = findViewById(R.id.button_write_qr_code);
        qrcodeImage = findViewById(R.id.image_view_write_qr_code);

        writer = new BarcodeWriter();
        reader = BarcodeReader.getInstance();

        writeBtn.setOnClickListener(this);
        qrcodeImage.setOnClickListener(this);

        onWrite();
    }

    public void onWrite() {
        String text = textEdit.getText().toString();
        writeAsync(text, qrcodeImage);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_write_qr_code) {
            onWrite();
        } else if (id == R.id.image_view_write_qr_code) {
            BitmapDrawable drawable = (BitmapDrawable) qrcodeImage.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            read(bitmap);
        }
    }

    public void read(Bitmap bitmap) {
        CodeResult result = reader.read(bitmap);
        if (result != null) {
            Toast.makeText(this, result.getText(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 生成二维码
     * <p>
     * 生成二维码是耗时操作，请务必放在子线程中执行
     *
     * @param text      文本
     * @param imageView 生成图片
     */
    private void writeAsync(final String text, final ImageView imageView) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        Observable
                .create(new ObservableOnSubscribe<Bitmap>() {
                    @Override
                    public void subscribe(ObservableEmitter<Bitmap> emitter) {
                        Bitmap bitmap = writer.write(text,
                                BarCodeUtil.dp2px(WriteQRCodeActivity.this, 200),
                                Color.BLACK);
                        if (bitmap != null) {
                            emitter.onNext(bitmap);
                        }
                        emitter.onComplete();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Bitmap>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("Write", "生成失败");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

}
