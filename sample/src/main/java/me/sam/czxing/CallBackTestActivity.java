package me.sam.czxing;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

//import me.devilsen.czxing.code.NativeSdk;

/**
 * desc:
 * date: 2019/08/14 0014
 *
 * @author : dongsen
 */
public class CallBackTestActivity extends AppCompatActivity {

//    private NativeSdk barcodeReader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_callback_test);

//        barcodeReader = new NativeSdk();
    }

    public void prepare(View view) {
//        barcodeReader.prepareTest();
    }

    public void test(View view) {
//        barcodeReader.callbackTestJava();
    }
}
