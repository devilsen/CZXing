package me.sam.czxing;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import me.devilsen.czxing.view.ScanBoxView;

/**
 * desc :
 * date : 2019-07-10 14:49
 *
 * @author : dongSen
 */
public class ScanBoxTestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScanBoxView scanBoxView = new ScanBoxView(this);
        scanBoxView.setBackgroundColor(Color.BLACK);
        setContentView(scanBoxView);

        Resources resources = getResources();
        scanBoxView.setCornerColor(resources.getColor(R.color.corner));
        scanBoxView.setBorderColor(resources.getColor(R.color.box_line));
        List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));
        scanBoxView.setScanLineColor(scanColors);
        scanBoxView.hideCardText();
        scanBoxView.setDark(true);

        scanBoxView.setScanBoxClickListener(new ScanBoxView.ScanBoxClickListener() {
            @Override
            public void onFlashLightClick() {
                Log.e("ScanBox", "onFlashLightClick");
            }
        });
    }
}
