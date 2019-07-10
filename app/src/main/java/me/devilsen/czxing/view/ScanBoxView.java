package me.devilsen.czxing.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
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
    private Paint mTxtPaint;
    private Paint mScanLinePaint;
    private Rect mFramingRect;
    private Rect mTextRect;

    private int mMaskColor;
    private int mTextColor;
    private int mTextColorBig;

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
    private int mTextSize;
    private int mTextSizeBig;

    private ScanBoxClickListener mFlashLightListener;
    // 是否处于黑暗环境
    private boolean isDark;

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
        mTxtPaint = new Paint();
        mTxtPaint.setAntiAlias(true);

        mMaskColor = Color.parseColor("#33000000");
        mTextColor = Color.parseColor("#FFF8F8F8");
        mTextColorBig = Color.parseColor("#BC0ED118");

        mBoxSize = BarCodeUtil.dp2px(context, 200);
        mTopOffset = -BarCodeUtil.dp2px(context, 60);

        mBorderColor = Color.WHITE;
        mBorderSize = BarCodeUtil.dp2px(context, 1);

        mCornerColor = Color.GREEN;
        mCornerLength = BarCodeUtil.dp2px(context, 20);
        mCornerSize = BarCodeUtil.dp2px(context, 3);
        mHalfCornerSize = 1.0f * mCornerSize / 2;

        mTextSize = BarCodeUtil.sp2px(context, 14);
        mTextSizeBig = BarCodeUtil.sp2px(context, 17);
        mTxtPaint.setTextSize(mTextSize);
        mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTxtPaint.setColor(Color.GRAY);
        mTxtPaint.setStyle(Paint.Style.FILL);
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

        // 画提示文本
        drawTipText(canvas);

        // 移动扫描线的位置
        moveScanLine();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (x > mFramingRect.left && x < mFramingRect.right &&
                    y > (mFramingRect.top + (mBoxSize >> 1)) && y < mFramingRect.bottom) {
                if (mFlashLightListener != null) {
                    mFlashLightListener.onFlashLightClick();
                    invalidate();
                }
                return true;
            }

            if (x > mTextRect.left && x < mTextRect.right &&
                    y > mTextRect.top && y < mTextRect.bottom) {
                if (mFlashLightListener != null) {
                    mFlashLightListener.onCardTextClick();
                }
                return true;
            }

        }
        return super.onTouchEvent(event);
    }

    public void setScanBoxClickListener(ScanBoxClickListener lightListener) {
        mFlashLightListener = lightListener;
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

    private void drawTipText(Canvas canvas) {
        mTxtPaint.setTextSize(mTextSize);
        mTxtPaint.setColor(mTextColor);
        if (isDark) {
            canvas.drawText("点击打开闪光灯",
                    mFramingRect.left + (mBoxSize >> 1),
                    mFramingRect.bottom - mTextSize,
                    mTxtPaint);
        }
        canvas.drawText("将二维码/条形码放入扫描框",
                mFramingRect.left + (mBoxSize >> 1),
                mFramingRect.bottom + mTextSize * 2,
                mTxtPaint);

        mTxtPaint.setTextSize(mTextSizeBig);
        mTxtPaint.setColor(mTextColorBig);
        String clickText = "我的名片";
        canvas.drawText(clickText,
                mFramingRect.left + (mBoxSize >> 1),
                mFramingRect.bottom + mTextSize * 6,
                mTxtPaint);

        if (mTextRect == null) {
            mTextRect = new Rect();
            mTxtPaint.getTextBounds(clickText, 0, clickText.length() - 1, mTextRect);
            int width = mTextRect.width();
            int height = mTextRect.height();
            mTextRect.left = mFramingRect.left + (mBoxSize >> 1) - 10;
            mTextRect.right = mTextRect.left + width + 10;
            mTextRect.top = mFramingRect.bottom + mTextSize * 6 - 10;
            mTextRect.bottom = mTextRect.top + height + 10;
        }
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

    public Point getScanBoxCenter() {
        int centerX = mBoxLeft + (mBoxSize >> 1);
        int centerY = mBoxTop + (mBoxSize >> 1);
        return new Point(centerX, centerY);
    }

    public void setDark(boolean dark) {
        if (this.isDark != dark) {
            invalidate();
        }
        isDark = dark;
    }

    public interface ScanBoxClickListener {
        void onFlashLightClick();

        void onCardTextClick();
    }
}
