package me.devilsen.czxing.view.scanview;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.List;

import me.devilsen.czxing.R;
import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * 新样式的扫描框
 */
public class ScanBoxView extends View {

    private static final int VERTICAL = 0;
    private static final int HORIZONTAL = 1;
    private final int SCAN_LINE_HEIGHT = BarCodeUtil.dp2px(getContext(), 4f);

    private Paint mPaint;
    private Paint mScanLinePaint;
    private final Rect mFramingRect = new Rect(0, 0, 1080, 1920);
    private Rect mFocusRect;
    private Canvas mCanvas;

    private int mTopOffset;

    private int mBorderColor;
    private float mBorderStrokeWidth;

    private LinearGradient mScanLineGradient;
    private float mScanLinePosition;
    private ValueAnimator mScanLineAnimator;

    private int mScanLineColor1;
    private int mScanLineColor2;
    private int mScanLineColor3;
    // 扫码线方向
    private int mScanlineOrientation;

    public ScanBoxView(Context context) {
        this(context, null);
    }

    public ScanBoxView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanBoxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Context context = getContext();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mScanLinePaint = new Paint();

        Resources resources = getResources();

        mScanLineColor1 = resources.getColor(R.color.czxing_scan_1);
        mScanLineColor2 = resources.getColor(R.color.czxing_scan_2);
        mScanLineColor3 = resources.getColor(R.color.czxing_scan_3);

        mTopOffset = BarCodeUtil.dp2px(context, 60);

        mBorderColor = resources.getColor(R.color.czxing_line_border);
        mBorderStrokeWidth = BarCodeUtil.dp2px(context, 0.5f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int scanLeft = 0;
        int scanTop = mTopOffset;
        Rect rect = new Rect(scanLeft, scanTop, scanLeft + w, scanTop + h * 2 / 3);
        mFramingRect.set(rect);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCanvas = canvas;

        // 画扫描线
        drawScanLine(canvas);

        // 移动扫描线的位置
        moveScanLine();
    }

    /**
     * 画扫描线
     */
    private void drawScanLine(Canvas canvas) {
        if (mScanLineGradient == null) {
            // 生成垂直方向的扫码线（从上往下）
            if (mScanlineOrientation == VERTICAL) {
                mScanLineGradient = new LinearGradient(mFramingRect.left, mFramingRect.top, mFramingRect.right, mFramingRect.top,
                        new int[]{mScanLineColor1, mScanLineColor2, mScanLineColor3, mScanLineColor2, mScanLineColor1},
                        null,
                        Shader.TileMode.CLAMP);
            } else { // 生成水平方向的扫码线（从左往右）
                mScanLineGradient = new LinearGradient(mFramingRect.left, mFramingRect.top, mFramingRect.left, mFramingRect.bottom,
                        new int[]{mScanLineColor1, mScanLineColor2, mScanLineColor3, mScanLineColor2, mScanLineColor1},
                        null,
                        Shader.TileMode.CLAMP);
            }
            mScanLinePaint.setShader(mScanLineGradient);
        }

        if (mScanlineOrientation == VERTICAL) {
            canvas.drawRect(mFramingRect.left,
                    mScanLinePosition,
                    mFramingRect.right,
                    mScanLinePosition + SCAN_LINE_HEIGHT,
                    mScanLinePaint);
        } else {
            canvas.drawRect(mScanLinePosition,
                    mFramingRect.top,
                    mScanLinePosition + SCAN_LINE_HEIGHT,
                    mFramingRect.bottom,
                    mScanLinePaint);
        }
    }

    /**
     * 画对焦部分的方框
     */
    public void drawFocusRect(int left, int top, int right, int bottom) {
        if (mFocusRect == null) {
            mFocusRect = new Rect(left, top, right, bottom);
        } else if (right != 0 && bottom != 0) {
            // 会比认为的高度高一点 大概 400px，这点还需改进
            mFocusRect.left = left;
            mFocusRect.top = top;
            mFocusRect.right = right;
            mFocusRect.bottom = bottom;

            BarCodeUtil.d("Focus location : left = " + left + " top = " + top +
                    " right = " + right + " bottom = " + bottom);
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mBorderColor);
        mPaint.setStrokeWidth(mBorderStrokeWidth);
        mCanvas.drawRect(mFocusRect, mPaint);
    }

    private void moveScanLine() {
        if (mScanLineAnimator != null && mScanLineAnimator.isRunning()) {
            return;
        }

        if (mScanlineOrientation == VERTICAL) {
            mScanLineAnimator = createAnimator(mFramingRect.top, mFramingRect.bottom);
        } else {
            mScanLineAnimator = createAnimator(mFramingRect.left, mFramingRect.right);
        }
        mScanLineAnimator.setDuration(2500);
        mScanLineAnimator.setInterpolator(new DecelerateInterpolator());
        mScanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mScanLineAnimator.start();
    }

    private ValueAnimator createAnimator(float start, float end) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScanLinePosition = (float) animation.getAnimatedValue();
                postInvalidateOnAnimation();
            }
        });
        return animator;
    }

    /**
     * 将扫码线设置为水平
     */
    public void setHorizontalScanLine() {
        this.mScanlineOrientation = HORIZONTAL;
    }

    /**
     * 设定扫描线的颜色
     *
     * @param colors 渐变颜色组合
     */
    public void setScanLineColor(List<Integer> colors) {
        if (colors == null || colors.size() < 3) {
            return;
        }

        mScanLineColor1 = colors.get(0);
        mScanLineColor2 = colors.get(1);
        mScanLineColor3 = colors.get(2);

        mScanLineGradient = null;
    }

    public void startAnim() {
        if (mScanLineAnimator != null && !mScanLineAnimator.isRunning()) {
            mScanLineAnimator.start();
        }
    }

    public void stopAnim() {
        if (mScanLineAnimator != null && mScanLineAnimator.isRunning()) {
            mScanLineAnimator.cancel();
        }
    }

    public void onDestroy() {
        if (mScanLineAnimator != null) {
            mScanLineAnimator.removeAllUpdateListeners();
            mScanLineAnimator = null;
        }
    }

    public Point getScanCenter() {
        return new Point(mFramingRect.centerX(), mFramingRect.centerY());
    }

    public Rect getScanBoxRect() {
        return mFramingRect;
    }

    public int getScanBoxSize() {
        return Math.min(mFramingRect.width(), mFramingRect.height());
    }

}
