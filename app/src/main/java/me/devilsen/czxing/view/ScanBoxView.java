package me.devilsen.czxing.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import me.devilsen.czxing.BarCodeUtil;
import me.devilsen.czxing.R;

/**
 * @author : dongSen
 * date : 2019-07-01 15:51
 * desc : 扫描框
 */
public class ScanBoxView extends View {

    private final int MAX_BOX_SIZE = BarCodeUtil.dp2px(getContext(), 300);
    private final int SCAN_LINE_HEIGHT = BarCodeUtil.dp2px(getContext(), 1.5f);

    private Paint mPaint;
    private Paint mScanLinePaint;
    private Rect mFramingRect;

    private int mMaskColor;

    private int mTopOffset;
    private int mBoxSize;

    private int mBorderColor;
    private float mBorderSize;

    private int mCornerColor;
    private int mCornerLength;
    private int mCornerSize;
    private float mHalfCornerSize;
    private int mBoxLeft;
    private int mBoxTop;

    private LinearGradient mScanLineGradient;
    private float mScanLinePosition;
    private ValueAnimator mScanLineAnimator;

    public ScanBoxView(Context context) {
        this(context, null);
    }

    public ScanBoxView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScanBoxView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Context context = getContext();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mScanLinePaint = new Paint();

        mMaskColor = Color.parseColor("#33000000");

        mBoxSize = BarCodeUtil.dp2px(context, 200);
        mTopOffset = -BarCodeUtil.dp2px(context, 60);

        mBorderColor = Color.WHITE;
        mBorderSize = BarCodeUtil.dp2px(context, 1);

        mCornerColor = Color.GREEN;
        mCornerLength = BarCodeUtil.dp2px(context, 20);
        mCornerSize = BarCodeUtil.dp2px(context, 3);
        mHalfCornerSize = 1.0f * mCornerSize / 2;

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calFramingRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFramingRect == null) {
            return;
        }

        // 画遮罩层
        drawMask(canvas);

        // 画边框线
        drawBorderLine(canvas);

        // 画四个直角的线
        drawCornerLine(canvas);

        // 画扫描线
        drawScanLine(canvas);
//
//        // 画提示文本
//        drawTipText(canvas);
//
        // 移动扫描线的位置
        moveScanLine();
    }

    private void calFramingRect() {
        if (mFramingRect != null) {
            return;
        }

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        mBoxSize = Math.min(viewWidth * 3 / 5, MAX_BOX_SIZE);
        mBoxLeft = (viewWidth - mBoxSize) / 2;
        mBoxTop = (viewHeight - mBoxSize) / 2 + mTopOffset;
        mFramingRect = new Rect(mBoxLeft, mBoxTop, mBoxLeft + mBoxSize, mBoxTop + mBoxSize);
    }

    private void drawMask(Canvas canvas) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (mMaskColor != Color.TRANSPARENT) {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mMaskColor);
            canvas.drawRect(0, 0, width, mFramingRect.top, mPaint);
            canvas.drawRect(0, mFramingRect.top, mFramingRect.left, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(mFramingRect.right + 1, mFramingRect.top, width, mFramingRect.bottom + 1, mPaint);
            canvas.drawRect(0, mFramingRect.bottom + 1, width, height, mPaint);
        }
    }

    private void drawBorderLine(Canvas canvas) {
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mBorderColor);
        mPaint.setStrokeWidth(mBorderSize);
        canvas.drawRect(mFramingRect, mPaint);
    }

    /**
     * 画四个直角的线
     */
    private void drawCornerLine(Canvas canvas) {
        if (mHalfCornerSize <= 0) {
            return;
        }
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mCornerColor);
        mPaint.setStrokeWidth(mCornerSize);
        canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.top, mFramingRect.left - mHalfCornerSize + mCornerLength, mFramingRect.top,
                mPaint);
        canvas.drawLine(mFramingRect.left, mFramingRect.top - mHalfCornerSize, mFramingRect.left, mFramingRect.top - mHalfCornerSize + mCornerLength,
                mPaint);
        canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.top, mFramingRect.right + mHalfCornerSize - mCornerLength, mFramingRect.top,
                mPaint);
        canvas.drawLine(mFramingRect.right, mFramingRect.top - mHalfCornerSize, mFramingRect.right, mFramingRect.top - mHalfCornerSize + mCornerLength,
                mPaint);

        canvas.drawLine(mFramingRect.left - mHalfCornerSize, mFramingRect.bottom, mFramingRect.left - mHalfCornerSize + mCornerLength,
                mFramingRect.bottom, mPaint);
        canvas.drawLine(mFramingRect.left, mFramingRect.bottom + mHalfCornerSize, mFramingRect.left,
                mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);
        canvas.drawLine(mFramingRect.right + mHalfCornerSize, mFramingRect.bottom, mFramingRect.right + mHalfCornerSize - mCornerLength,
                mFramingRect.bottom, mPaint);
        canvas.drawLine(mFramingRect.right, mFramingRect.bottom + mHalfCornerSize, mFramingRect.right,
                mFramingRect.bottom + mHalfCornerSize - mCornerLength, mPaint);
    }

    /**
     * 画扫描线
     */
    private void drawScanLine(Canvas canvas) {
        if (mScanLineGradient == null) {
            Resources resources = getResources();
            int color1 = resources.getColor(R.color.color1);
            int color2 = resources.getColor(R.color.color2);
            int color3 = resources.getColor(R.color.color3);

            mScanLineGradient = new LinearGradient(mBoxLeft, mBoxTop, mBoxLeft + mBoxSize, mBoxTop,
                    new int[]{color1, color2, color3, color2, color1},
                    null,
                    Shader.TileMode.CLAMP);
            mScanLinePaint.setShader(mScanLineGradient);
        }

        canvas.drawRect(mBoxLeft,
                mBoxTop + mScanLinePosition,
                mBoxLeft + mBoxSize,
                mBoxTop + mScanLinePosition + SCAN_LINE_HEIGHT,
                mScanLinePaint);
    }

    private void moveScanLine() {
        if (mScanLineAnimator != null && mScanLineAnimator.isRunning()) {
            return;
        }
        mScanLineAnimator = ValueAnimator.ofFloat(0, mBoxSize - mBorderSize * 2);
        mScanLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScanLinePosition = (float) animation.getAnimatedValue();
//                postInvalidate();
                postInvalidate(mBoxLeft,
                        ((int) (mBoxTop + mScanLinePosition - 10)),
                        mBoxLeft + mBoxSize,
                        ((int) (mBoxTop + mScanLinePosition + SCAN_LINE_HEIGHT + 10)));
            }
        });
        mScanLineAnimator.setDuration(2500);
        mScanLineAnimator.setInterpolator(new LinearInterpolator());
        mScanLineAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mScanLineAnimator.start();
    }

    public Rect getScanBoxRect() {
        return mFramingRect;
    }

    public int getScanBoxSize() {
        return mBoxSize;
    }
}
