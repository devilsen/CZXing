package me.devilsen.czxing.view.scanview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import me.devilsen.czxing.code.BarcodeFormat;
import me.devilsen.czxing.code.BarcodeReader;
import me.devilsen.czxing.code.CodeResult;
import me.devilsen.czxing.tflite.Classifier;
import me.devilsen.czxing.tflite.YoloV4Classifier;
import me.devilsen.czxing.thread.ExecutorUtil;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ImageUtils;
import me.devilsen.czxing.view.tracking.BorderedText;
import me.devilsen.czxing.view.tracking.MultiBoxTracker;
import me.devilsen.czxing.view.tracking.OverlayView;

import static me.devilsen.czxing.tflite.YoloV4Classifier.MINIMUM_CONFIDENCE_TF_OD_API;
import static me.devilsen.czxing.util.BarCodeUtil.isDebug;

/**
 * @author : dongSen
 * date : 2020-12-16
 * desc : 二维码界面使用类
 */
public class TFLiteScanView extends BarCoderView implements ScanBoxView.ScanBoxClickListener,
        BarcodeReader.ReadCodeListener {

    /** 混合扫描模式（默认），扫描4次扫码框里的内容，扫描1次以屏幕宽为边长的内容 */
    public static final int SCAN_MODE_MIX = 0;
    /** 只扫描扫码框里的内容 */
    public static final int SCAN_MODE_TINY = 1;
    /** 扫描以屏幕宽为边长的内容 */
    public static final int SCAN_MODE_BIG = 2;

    private static final int DARK_LIST_SIZE = 4;

    private static final float TEXT_SIZE_DIP = 10;
    private static final int TF_OD_API_INPUT_SIZE = 416;
    private static final String TF_OD_API_MODEL_FILE = "yolov4-416-fp32.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco.txt";
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final boolean SAVE_PREVIEW_BITMAP = false;

    private boolean isStop;
    private boolean isDark;
    private int showCounter;
    private final BarcodeReader reader;
    private ScanListener.AnalysisBrightnessListener brightnessListener;

    private Classifier detector;

    private OverlayView trackingOverlay;
    private MultiBoxTracker tracker;
    private BorderedText borderedText;

    private Integer sensorOrientation;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private long timestamp = 0;
    private boolean computingDetection = false;
    private long lastProcessingTimeMs;

    public TFLiteScanView(Context context) {
        this(context, null);
    }

    public TFLiteScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TFLiteScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScanBoxView.setScanBoxClickListener(this);
        reader = BarcodeReader.getInstance();

        trackingOverlay = new OverlayView(context, attrs);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(trackingOverlay, params);
    }

    @Override
    public void onPreviewFrame(byte[] data, int left, int top, int width, int height, int rowWidth, int rowHeight) {
        if (isStop) {
            return;
        }
        reader.read(data, left, top, width, height, rowWidth, rowHeight);
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
    protected void onPreviewSizeChosen(int previewWidth, int previewHeight, int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(getContext());

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    YoloV4Classifier.create(
                            getContext().getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            BarCodeUtil.e(e.toString() + "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            if (mScanListener != null) {
                mScanListener.onOpenCameraError();
            }
        }

        sensorOrientation = rotation - getScreenOrientation();
        BarCodeUtil.i("Camera orientation relative to screen canvas:" + sensorOrientation);
        BarCodeUtil.i("Initializing at size " + previewWidth + "x" + previewHeight);

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        BarCodeUtil.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        ExecutorUtil.runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        BarCodeUtil.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        final List<Classifier.Recognition> mappedRecognitions = new LinkedList<>();
                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

//                        ExecutorUtil.runOnUiThread(
//                                new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        showFrameInfo(previewWidth + "x" + previewHeight);
//                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                                        showInference(lastProcessingTimeMs + "ms");
//                                    }
//                                });
                        printDetectInfo();
                    }
                });
    }

    private void printDetectInfo() {
        BarCodeUtil.d("Frame Size: " + previewWidth + "x" + previewHeight);
        BarCodeUtil.d("Crop Size : " + cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
        BarCodeUtil.d("Time      : " + lastProcessingTimeMs + "ms");
    }

    @Override
    public void onReadCodeResult(CodeResult result) {
        if (result == null) {
            return;
        }
//        showCodeBorder(result);
        BarCodeUtil.d("result : " + result.toString());

        if (!TextUtils.isEmpty(result.getText()) && !isStop) {
            isStop = true;
            reader.stopRead();
            if (mScanListener != null) {
                mScanListener.onScanSuccess(result.getText(), result.getFormat());
            }
        } else if (result.getPoints() != null) {
            tryZoom(result);
        }
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

    protected int getScreenOrientation() {
//        switch (getWindowManager().getDefaultDisplay().getRotation()) {
//            case Surface.ROTATION_270:
//                return 270;
//            case Surface.ROTATION_180:
//                return 180;
//            case Surface.ROTATION_90:
//                return 90;
//            default:
//                return 0;
//        }
        return 0;
    }

}
