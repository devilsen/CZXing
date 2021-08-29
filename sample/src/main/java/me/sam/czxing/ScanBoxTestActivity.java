package me.sam.czxing;

import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.camera.camera2.ScanCamera2;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * desc :
 * date : 2019-07-10 14:49
 *
 * @author : dongSen
 */
public class ScanBoxTestActivity extends AppCompatActivity {

    private AutoFitSurfaceView mSurface;
    private ScanCamera camera2;
    private BarcodeReader reader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_camera2_surface);
//        ScanBoxView scanBoxView = new ScanBoxView(this);
//        scanBoxView.setBackgroundColor(Color.BLACK);
//        setContentView(scanBoxView);
//
//        Resources resources = getResources();
//        scanBoxView.setCornerColor(resources.getColor(R.color.corner));
//        scanBoxView.setBorderColor(resources.getColor(R.color.box_line));
//        List<Integer> scanColors = Arrays.asList(resources.getColor(R.color.scan_side), resources.getColor(R.color.scan_partial), resources.getColor(R.color.scan_middle));
//        scanBoxView.setScanLineColor(scanColors);
//        scanBoxView.hideCardText();
//        scanBoxView.setDark(true);
////        scanBoxView.setBorderSize(BarCodeUtil.dp2px(this, 200));
//        scanBoxView.setBorderSize(BarCodeUtil.dp2px(this, 200),BarCodeUtil.dp2px(this, 100));
//        scanBoxView.setBoxTopOffset(-BarCodeUtil.dp2px(this, 100));
//        scanBoxView.setFlashLightOnDrawable(R.drawable.ic_highlight_blue_open_24dp);
//        scanBoxView.setFlashLightOffDrawable(R.drawable.ic_highlight_white_close_24dp);
//        scanBoxView.invisibleFlashLightIcon();
//        scanBoxView.setMaskColor(Color.parseColor("#860036F8"));
//
//        scanBoxView.setScanBoxClickListener(new ScanBoxView.ScanBoxClickListener() {
//            @Override
//            public void onFlashLightClick() {
//                Log.e("ScanBox", "onFlashLightClick");
//            }
//        });
        reader = BarcodeReader.getInstance();
        testCamera2();
    }

    private void testCamera2() {
        mSurface = findViewById(R.id.camera2_auto_surface);

        camera2 = new ScanCamera2(this, mSurface);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2.onCreate();
        }

        camera2.setPreviewListener(new ScanCamera.ScanPreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, int rowWidth, int rowHeight) {
                int bisSize = Math.min(rowWidth, rowHeight);
                reader.read(data, 0, 0, bisSize, bisSize, rowWidth, rowHeight);
            }
        });

        reader.setReadCodeListener(new BarcodeReader.ReadCodeListener() {

            @Override
            public void onReadCodeResult(List<CodeResult> resultList) {
                for (CodeResult result : resultList) {
                    BarCodeUtil.d("result : " + result.toString());
                }
            }

            @Override
            public void onFocus() {
                BarCodeUtil.d("not found code too many times , try focus");
            }

            @Override
            public void onAnalysisBrightness(boolean isDark) {
                BarCodeUtil.d("isDark : " + isDark);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reader.prepareRead();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2.onPause();
        }
        reader.stopRead();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2.onDestroy();
        }
    }

    public void openFlash(View view) {
        camera2.openFlashlight();
    }

    public void closeFlash(View view) {
        camera2.closeFlashlight();
    }

    public void focusTop(View view) {
        camera2.focus(540, 0);
    }

    public void focusBottom(View view) {
        camera2.focus(540, 1920);
    }

    private int zoomValue = 1;

    public void zoomIn(View view) {
        zoomValue = zoomValue + 5;
        zoomValue = (int) camera2.zoom(zoomValue);
    }

    public void zoomOut(View view) {
        zoomValue = zoomValue - 5;
        zoomValue = (int) camera2.zoom(zoomValue);
    }
}
