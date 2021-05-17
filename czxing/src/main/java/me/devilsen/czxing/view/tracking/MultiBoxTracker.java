package me.devilsen.czxing.view.tracking;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.ImageUtils;

/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {

//    private static final float TEXT_SIZE_DIP = 18;
//    private static final float MIN_SIZE = 16.0f;
//    private static final int[] COLORS = {
//            Color.BLUE,
//            Color.RED,
//            Color.GREEN,
//            Color.YELLOW,
//            Color.CYAN,
//            Color.MAGENTA,
//            Color.WHITE,
//            Color.parseColor("#55FF55"),
//            Color.parseColor("#FFA500"),
//            Color.parseColor("#FF8888"),
//            Color.parseColor("#AAAAFF"),
//            Color.parseColor("#FFFFAA"),
//            Color.parseColor("#55AAAA"),
//            Color.parseColor("#AA33AA"),
//            Color.parseColor("#0D0068")
//    };
//    final List<Pair<Float, RectF>> screenRects = new LinkedList<>();
//    private final Queue<Integer> availableColors = new LinkedList<>();
//    private final List<TrackedRecognition> trackedObjects = new LinkedList<>();
//    private final Paint boxPaint = new Paint();
//    private final float textSizePx;
//    private final BorderedText borderedText;
//    private Matrix frameToCanvasMatrix;
//    private int frameWidth;
//    private int frameHeight;
//    private int sensorOrientation;
//
//    public MultiBoxTracker(final Context context) {
//        for (final int color : COLORS) {
//            availableColors.add(color);
//        }
//
//        boxPaint.setColor(Color.RED);
//        boxPaint.setStyle(Style.STROKE);
//        boxPaint.setStrokeWidth(10.0f);
//        boxPaint.setStrokeCap(Cap.ROUND);
//        boxPaint.setStrokeJoin(Join.ROUND);
//        boxPaint.setStrokeMiter(100);
//
//        textSizePx =
//                TypedValue.applyDimension(
//                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
//        borderedText = new BorderedText(textSizePx);
//    }
//
//    public synchronized void setFrameConfiguration(
//            final int width, final int height, final int sensorOrientation) {
//        frameWidth = width;
//        frameHeight = height;
//        this.sensorOrientation = sensorOrientation;
//    }
//
//    public synchronized void drawDebug(final Canvas canvas) {
//        final Paint textPaint = new Paint();
//        textPaint.setColor(Color.WHITE);
//        textPaint.setTextSize(60.0f);
//
//        final Paint boxPaint = new Paint();
//        boxPaint.setColor(Color.RED);
//        boxPaint.setAlpha(200);
//        boxPaint.setStyle(Style.STROKE);
//
//        for (final Pair<Float, RectF> detection : screenRects) {
//            final RectF rect = detection.second;
//            canvas.drawRect(rect, boxPaint);
//            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
//            borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
//        }
//    }
//
//    public synchronized void trackResults(final List<Classifier.Recognition> results, final long timestamp) {
//        BarCodeUtil.i("Processing " + results.size() + " results from " + timestamp);
//        processResults(results);
//    }
//
//    private Matrix getFrameToCanvasMatrix() {
//        return frameToCanvasMatrix;
//    }
//
//    public synchronized void draw(final Canvas canvas) {
//        final boolean rotated = sensorOrientation % 180 == 90;
//        final float multiplier =
//                Math.min(
//                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
//                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
//        frameToCanvasMatrix =
//                ImageUtils.getTransformationMatrix(
//                        frameWidth,
//                        frameHeight,
//                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
//                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
//                        sensorOrientation,
//                        false);
//
//        for (final TrackedRecognition recognition : trackedObjects) {
//            final RectF trackedPos = new RectF(recognition.location);
//
//            getFrameToCanvasMatrix().mapRect(trackedPos);
//            boxPaint.setColor(recognition.color);
//
//            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
//            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
//
//            final String labelString =
//                    !TextUtils.isEmpty(recognition.title)
//                            ? String.format("%s %.2f", recognition.title, (100 * recognition.detectionConfidence))
//                            : String.format("%.2f", (100 * recognition.detectionConfidence));
//            //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
//            // labelString);
//            borderedText.drawText(
//                    canvas, trackedPos.left + cornerSize, trackedPos.top, labelString + "%", boxPaint);
//        }
//    }
//
//    private void processResults(final List<Classifier.Recognition> results) {
//        final List<Pair<Float, Classifier.Recognition>> rectsToTrack = new LinkedList<>();
//
//        screenRects.clear();
//        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());
//
//        for (final Classifier.Recognition result : results) {
//            if (result.getLocation() == null) {
//                continue;
//            }
//            final RectF detectionFrameRect = new RectF(result.getLocation());
//
//            final RectF detectionScreenRect = new RectF();
//            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);
//
//            BarCodeUtil.i(
//                    "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);
//
//            screenRects.add(new Pair<>(result.getConfidence(), detectionScreenRect));
//
//            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
//                BarCodeUtil.w("Degenerate rectangle! " + detectionFrameRect);
//                continue;
//            }
//
//            rectsToTrack.add(new Pair<>(result.getConfidence(), result));
//        }
//
//        trackedObjects.clear();
//        if (rectsToTrack.isEmpty()) {
//            BarCodeUtil.i("Nothing to track, aborting.");
//            return;
//        }
//
//        for (final Pair<Float, Classifier.Recognition> potential : rectsToTrack) {
//            final TrackedRecognition trackedRecognition = new TrackedRecognition();
//            trackedRecognition.detectionConfidence = potential.first;
//            trackedRecognition.location = new RectF(potential.second.getLocation());
//            trackedRecognition.title = potential.second.getTitle();
//            trackedRecognition.color = COLORS[trackedObjects.size()];
//            trackedObjects.add(trackedRecognition);
//
//            if (trackedObjects.size() >= COLORS.length) {
//                break;
//            }
//        }
//    }
//
//    private static class TrackedRecognition {
//        RectF location;
//        float detectionConfidence;
//        int color;
//        String title;
//    }
}
