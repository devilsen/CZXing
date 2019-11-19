package me.devilsen.czxing.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
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

import java.util.List;

import me.devilsen.czxing.R;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;

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
    private int mBoxSizeOffset;

    private int mBorderColor;
    private int mBoxSize;
    private float mBorderStrokeWidth;

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
    private boolean mDrawCardText;
    private boolean isLightOn;

    private int mScanLineColor1;
    private int mScanLineColor2;
    private int mScanLineColor3;

    private Bitmap mLightOn;
    private Bitmap mLightOff;
    private int mFlashLightLeft;
    private int mFlashLightTop;
    private int mFlashLightRight;
    private int mFlashLightBottom;
    // 不使用手电筒图标
    private boolean mDropFlashLight;

    private String mFlashLightOnText;
    private String mFlashLightOffText;
    private String mScanNoticeText;

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
        mTxtPaint = new Paint();
        mTxtPaint.setAntiAlias(true);

        Resources resources = getResources();

        mMaskColor = resources.getColor(R.color.czxing_line_mask);
        mTextColor = resources.getColor(R.color.czxing_text_normal);
        mTextColorBig = resources.getColor(R.color.czxing_text_big);

        mScanLineColor1 = resources.getColor(R.color.czxing_scan_1);
        mScanLineColor2 = resources.getColor(R.color.czxing_scan_2);
        mScanLineColor3 = resources.getColor(R.color.czxing_scan_3);

        mTopOffset = -BarCodeUtil.dp2px(context, 10);
        mBoxSizeOffset = BarCodeUtil.dp2px(context, 40);

        mBorderColor = resources.getColor(R.color.czxing_line_border);
        mBorderStrokeWidth = BarCodeUtil.dp2px(context, 0.5f);

        mCornerColor = resources.getColor(R.color.czxing_line_corner);
        mCornerLength = BarCodeUtil.dp2px(context, 20);
        mCornerSize = BarCodeUtil.dp2px(context, 3);
        mHalfCornerSize = 1.0f * mCornerSize / 2;

        mTextSize = BarCodeUtil.sp2px(context, 14);
        mTextSizeBig = BarCodeUtil.sp2px(context, 17);
        mTxtPaint.setTextSize(mTextSize);
        mTxtPaint.setTextAlign(Paint.Align.CENTER);
        mTxtPaint.setColor(Color.GRAY);
        mTxtPaint.setStyle(Paint.Style.FILL);

        mFlashLightOnText = getResources().getText(R.string.czxing_click_open_flash_light).toString();
        mFlashLightOffText = getResources().getText(R.string.czxing_click_close_flash_light).toString();
        mScanNoticeText = getResources().getText(R.string.czxing_scan_notice).toString();
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

            if (x > mFlashLightLeft && x < mFlashLightRight &&
                    y > mFlashLightTop && y < mFlashLightBottom) {
                // 在亮度不够的情况下，或者在打开闪光灯的情况下才可以点击
                if (mFlashLightListener != null && (isDark || isLightOn)) {
                    mFlashLightListener.onFlashLightClick();
                    isLightOn = !isLightOn;
                    invalidate();
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
        int minSize = Math.min(viewHeight, viewWidth);
        if (mBoxSize == 0) {
            mBoxSize = Math.min(minSize * 3 / 5, MAX_BOX_SIZE);
        } else if (mBoxSize > minSize) {
            mBoxSize = minSize;
        }
        mBoxLeft = (viewWidth - mBoxSize) / 2;
        mBoxTop = (viewHeight - mBoxSize) / 2 + mTopOffset;
        mBoxTop = mBoxTop < 0 ? 0 : mBoxTop;
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
        mPaint.setStrokeWidth(mBorderStrokeWidth);
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
            mScanLineGradient = new LinearGradient(mBoxLeft, mBoxTop, mBoxLeft + mBoxSize, mBoxTop,
                    new int[]{mScanLineColor1, mScanLineColor2, mScanLineColor3, mScanLineColor2, mScanLineColor1},
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

        if (!mDropFlashLight) {
            if (isDark || isLightOn) {
                canvas.drawText(isLightOn ? mFlashLightOffText : mFlashLightOnText,
                        mFramingRect.left + (mBoxSize >> 1),
                        mFramingRect.bottom - mTextSize,
                        mTxtPaint);

                drawFlashLight(canvas);
            }
        }

        canvas.drawText(mScanNoticeText,
                mFramingRect.left + (mBoxSize >> 1),
                mFramingRect.bottom + mTextSize * 2,
                mTxtPaint);

        // 隐藏 我的卡片 文字
        if (!mDrawCardText) {
            return;
        }

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

    /**
     * 画手电筒
     */
    private void drawFlashLight(Canvas canvas) {
        // 不使用手电筒图标
        if (mDropFlashLight) {
            return;
        }
        if (mLightOff == null) {
            mLightOff = BitmapUtil.getBitmap(getContext(), R.drawable.ic_highlight_black_close_24dp);
        }
        if (mLightOn == null) {
            mLightOn = BitmapUtil.getBitmap(getContext(), R.drawable.ic_highlight_black_open_24dp);
        }
        if (mFlashLightLeft == 0 && mLightOff != null) {
            mFlashLightLeft = mFramingRect.left + ((mFramingRect.width() - mLightOff.getWidth()) >> 1);
            mFlashLightTop = mFramingRect.bottom - (mTextSize << 2);
            mFlashLightRight = mFlashLightLeft + mLightOff.getWidth();
            mFlashLightBottom = mFlashLightTop + mLightOff.getHeight();
        }
        drawFlashLightBitmap(canvas);
    }

    private void drawFlashLightBitmap(Canvas canvas) {
        if (isLightOn) {
            if (mLightOn != null) {
                canvas.drawBitmap(mLightOn, mFlashLightLeft, mFlashLightTop, mPaint);
            }
        } else {
            if (mLightOff != null) {
                canvas.drawBitmap(mLightOff, mFlashLightLeft, mFlashLightTop, mPaint);
            }
        }
    }

    private void moveScanLine() {
        if (mScanLineAnimator != null && mScanLineAnimator.isRunning()) {
            return;
        }
        mScanLineAnimator = ValueAnimator.ofFloat(0, mBoxSize - mBorderStrokeWidth * 2);
        mScanLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScanLinePosition = (float) animation.getAnimatedValue();
                // 这里如果用postInvalidate会导致所在Activity的onStop和onDestroy方法阻塞，感谢lhhseraph的反馈
                postInvalidateOnAnimation(mBoxLeft,
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

    /**
     * 有的手机得到的数据会有所偏移（如：华为P20），这里放大了获取到的数据
     */
    public int getScanBoxSizeExpand() {
        return mBoxSize + mBoxSizeOffset;
    }

    /**
     * 获取数据偏移量
     */
    public int getExpandTop() {
        return mBoxSizeOffset;
    }

    public Point getScanBoxCenter() {
        int centerX = mBoxLeft + (mBoxSize >> 1);
        int centerY = mBoxTop + (mBoxSize >> 1);
        return new Point(centerX, centerY);
    }

    public void setDark(boolean dark) {
        if (this.isDark != dark) {
            postInvalidate();
        }
        isDark = dark;
    }

    /**
     * 设定四个角的颜色
     */
    public void setCornerColor(int color) {
        if (color == 0) {
            return;
        }
        this.mCornerColor = color;
    }

    /**
     * 设定扫描框的边框颜色
     */
    public void setBorderColor(int color) {
        if (color == 0) {
            return;
        }
        this.mBorderColor = color;
    }

    /**
     * 设置边框长度
     *
     * @param borderSize px
     */
    public void setBorderSize(int borderSize) {
        if (borderSize <= 0) {
            return;
        }
        this.mBoxSize = borderSize;
    }


    /**
     * 设置边框长度
     *
     * @param strokeWidth px
     */
    public void setBorderStrokeWidth(int strokeWidth) {
        if (strokeWidth <= 0) {
            return;
        }
        this.mBorderStrokeWidth = strokeWidth;
    }

    /**
     * 设置扫码框上下偏移量，可以为负数
     *
     * @param offset px
     */
    public void setBoxTopOffset(int offset) {
        mTopOffset = offset;
    }

    /**
     * 设置扫码框四周的颜色
     *
     * @param color 透明颜色
     */
    public void setMaskColor(int color) {
        mMaskColor = color;
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

    /**
     * 设置手电筒打开时的图标
     */
    public void setFlashLightOnDrawable(int lightOnDrawable) {
        mLightOn = BitmapUtil.getBitmap(getContext(), lightOnDrawable);
    }

    /**
     * 设置手电筒关闭时的图标
     */
    public void setFlashLightOffDrawable(int lightOffDrawable) {
        mLightOff = BitmapUtil.getBitmap(getContext(), lightOffDrawable);
    }

    /**
     * 不使用手电筒图标及提示
     */
    public void invisibleFlashLightIcon() {
        mDropFlashLight = true;
    }

    /**
     * 隐藏 我的卡片 功能
     */
    public void hideCardText() {
        this.mDrawCardText = false;
    }

    /**
     * 设置闪光灯打开时的提示文字
     */
    public void setFlashLightOnText(String lightOnText) {
        if (lightOnText != null) {
            mFlashLightOnText = lightOnText;
        }
    }

    /**
     * 设置闪光灯关闭时的提示文字
     */
    public void setFlashLightOffText(String lightOffText) {
        if (lightOffText != null) {
            mFlashLightOffText = lightOffText;
        }
    }

    /**
     * 设置扫码框下方的提示文字
     */
    public void setScanNoticeText(String scanNoticeText) {
        if (scanNoticeText != null) {
            mScanNoticeText = scanNoticeText;
        }
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
        }
    }

    public interface ScanBoxClickListener {
        void onFlashLightClick();
    }
}
