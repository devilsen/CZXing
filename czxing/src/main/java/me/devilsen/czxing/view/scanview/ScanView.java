package me.devilsen.czxing.view.scanview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.view.PointView;

/**
 * @author : dongSen
 * date : 2019-06-29 16:18
 * desc : 二维码界面使用类
 */
public class ScanView extends BarCoderView implements BarcodeReader.ReadCodeListener, ScanBoxView.ScanBoxClickListener {

    /**
     * 混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容
     */
    public static final int SCAN_MODE_MIX = 0;
    /**
     * 只扫描扫码框里的内容
     */
    public static final int SCAN_MODE_TINY = 1;
    /**
     * 扫描以屏幕宽为边长的内容
     */
    public static final int SCAN_MODE_BIG = 2;

    private static final int DARK_LIST_SIZE = 4;

    private boolean isStop;
    private boolean isDark;
    private int showCounter;
    private int mResultColor;
    private boolean mIsHideResultColor;
    private int mPointSize;

    private final BarcodeReader reader;
    private ScanListener.AnalysisBrightnessListener brightnessListener;
    private final List<View> resultViews = new ArrayList<>();

    public ScanView(Context context) {
        this(context, null);
    }

    public ScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScanBoxView.setScanBoxClickListener(this);
        reader = BarcodeReader.getInstance();
        mPointSize = BarCodeUtil.dp2px(context, 15);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }
        List<CodeResult> codeResult = reader.read(data, left, top, width, height, rowWidth, rowHeight);
        onReadCodeResult(codeResult);
//        Log.e("save >>> ", "left = " + left + " top= " + top +
//                " width=" + width + " height= " + height + " rowWidth=" + rowWidth + " rowHeight=" + rowHeight);

//        SaveImageUtil.saveData(getContext(), data, left, top, width, height, rowWidth);
    }

    /**
     * 设置亮度分析回调
     */
    public void setAnalysisBrightnessListener(ScanListener.AnalysisBrightnessListener brightnessListener) {
        this.brightnessListener = brightnessListener;
    }

    /**
     * 设置扫描格式
     */
    public void setBarcodeFormat(BarcodeFormat... formats) {
        if (formats == null || formats.length == 0) {
            return;
        }
        reader.setBarcodeFormat(formats);
    }

    @Override
    public void startScan() {
        reader.setReadCodeListener(this);
        super.startScan();
        reader.prepareRead();
        isStop = false;
    }

    @Override
    public void stopScan() {
        super.stopScan();
        reader.stopRead();
        isStop = true;
        reader.setReadCodeListener(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onReadCodeResult(List<CodeResult> resultList) {
        if (resultList.size() == 0) {
            return;
        }
//        showCodeBorder(result);
        for (CodeResult result : resultList) {
            BarCodeUtil.d("result : " + result.toString());
        }

        showResultPoint(resultList);

        if (!isStop) {
            isStop = true;
            reader.stopRead();
            if (mScanListener != null) {
                mScanListener.onScanSuccess(resultList);
            }
        }

//        else if (result.getPoints() != null) {
//            tryZoom(result);
//        }
    }

    private void showResultPoint(List<CodeResult> resultList) {
        if (mIsHideResultColor) return;

        removeResultViews();

        for (CodeResult result : resultList) {
            addPointView(result, resultList.size());
        }
    }

    private void removeResultViews() {
        for (View view : resultViews) {
            removeView(view);
        }
    }

    private void addPointView(final CodeResult result, int resultSize) {
        int[] points = result.getPoints();
        if (points == null || points.length < 4) return;
        int x = points[0];
        int y = points[1];
        int width = points[2];
        int height = points[3];

        PointView view = new PointView(getContext());
        if (resultSize > 1) {
            view.drawArrow();
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mScanListener != null) {
                        mScanListener.onClickResult(result);
                    }
                }
            });
        }
        resultViews.add(view);
        if (mResultColor > 0) {
            view.setColor(mResultColor);
        }

        int xOffset = (width - mPointSize) / 2;
        int yOffset = (height - mPointSize) / 2;

        xOffset = Math.max(xOffset, 0);
        yOffset = Math.max(yOffset, 0);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.leftMargin = x + xOffset;
        params.topMargin = y + yOffset;

        addView(view, params);
    }

    public void setResultColor(int resultColor) {
        mResultColor = resultColor;
    }

    public void hideResultColor(boolean hideResultColor) {
        mIsHideResultColor = hideResultColor;
    }

    @Override
    public void onFocus() {
        BarCodeUtil.d("not found code too many times , try focus");
        mCamera.onFrozen();
    }

    @Override
    public void onAnalysisBrightness(boolean isDark) {
        BarCodeUtil.d("isDark : " + isDark);

        if (isDark) {
            showCounter++;
            showCounter = Math.min(showCounter, DARK_LIST_SIZE);
        } else {
            showCounter--;
            showCounter = Math.max(showCounter, 0);
        }

        if (this.isDark) {
            if (showCounter <= 2) {
                this.isDark = false;
                mScanBoxView.setDark(false);
            }
        } else {
            if (showCounter >= DARK_LIST_SIZE) {
                this.isDark = true;
                mScanBoxView.setDark(true);
            }
        }

        if (brightnessListener != null) {
            brightnessListener.onAnalysisBrightness(this.isDark);
        }
    }

    @Override
    public void onFlashLightClick() {
        if (mCamera.isFlashLighting()) {
            closeFlashlight();
        } else if (isDark) {
            openFlashlight();
        }
    }
}
