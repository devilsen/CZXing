package me.devilsen.czxing;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import me.devilsen.czxing.view.ScanBoxView;

/**
 * desc :
 * date : 2019-07-10 14:49
 *
 * @author : dongSen
 */
public class ScanBoxTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanBoxView scanBoxView = new ScanBoxView(this);
        setContentView(scanBoxView);

        scanBoxView.setScanBoxClickListener(new ScanBoxView.ScanBoxClickListener() {
            @Override
            public void onFlashLightClick() {
                Log.e("ScanBox","onFlashLightClick");
            }

            @Override
            public void onCardTextClick() {
                Log.e("ScanBox","onCardTextClick");

            }
        });
    }
}
