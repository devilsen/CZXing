package me.devilsen.czxing.view.scanview;

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
import android.view.animation.DecelerateInterpolator;

import java.util.List;

import me.devilsen.czxing.R;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.BitmapUtil;

/**
 * @author : dongSen
 * 新样式的扫描框
 */
public class ScanBoxView extends View {

    private static final int VERTICAL = 0;
    private static final int HORIZONTAL = 1;
    private final int SCAN_LINE_HEIGHT = BarCodeUtil.dp2px(getContext(), 4f);

    private Paint mPaint;
    private Paint mTxtPaint;
    private Paint mScanLinePaint;
    private final Rect mFramingRect = new Rect(0, 0, 1080, 1920);
    private Rect mFocusRect;
    private Rect mTextRect;
    private Canvas mCanvas;

    private int mMaskColor;
    private int mTextColor;
    private int mTextColorBig;

    private int mTopOffset;

    private int mBorderColor;
    private float mBorderStrokeWidth;

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
    // 扫码线方向
    private int mScanlineOrientation;

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

    private int mScanLeft;
    private int mScanTop;

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

        mTopOffset = BarCodeUtil.dp2px(context, 60);

        mBorderColor = resources.getColor(R.color.czxing_line_border);
        mBorderStrokeWidth = BarCodeUtil.dp2px(context, 0.5f);

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

        mScanLeft = 0;
        mScanTop = mTopOffset;
        Rect rect = new Rect(mScanLeft, mScanTop, mScanLeft + w, mScanTop + h * 2 / 3);
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

    private void drawTipText(Canvas canvas) {
        mTxtPaint.setTextSize(mTextSize);
        mTxtPaint.setColor(mTextColor);

        if (!mDropFlashLight) {
            if (isDark || isLightOn) {
                canvas.drawText(isLightOn ? mFlashLightOffText : mFlashLightOnText,
                        mFramingRect.left,
                        mFramingRect.bottom - mTextSize,
                        mTxtPaint);

                drawFlashLight(canvas);
            }
        }

        canvas.drawText(mScanNoticeText,
                mFramingRect.left,
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
                mFramingRect.left,
                mFramingRect.bottom + mTextSize * 6,
                mTxtPaint);

        if (mTextRect == null) {
            mTextRect = new Rect();
            mTxtPaint.getTextBounds(clickText, 0, clickText.length() - 1, mTextRect);
            int width = mTextRect.width();
            int height = mTextRect.height();
            mTextRect.left = mFramingRect.left - 10;
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
            mLightOff = BitmapUtil.getBitmap(getContext(), R.drawable.ic_highlight_close_24dp);
        }
        if (mLightOn == null) {
            mLightOn = BitmapUtil.getBitmap(getContext(), R.drawable.ic_highlight_open_24dp);
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

    public void setDark(boolean dark) {
        if (this.isDark != dark) {
            postInvalidate();
        }
        isDark = dark;
    }

    /**
     * 将扫码线设置为水平
     */
    public void setHorizontalScanLine() {
        this.mScanlineOrientation = HORIZONTAL;
    }

    /**
     * 设置扫码得到结果后的遮罩颜色
     *
     * @param color 透明颜色
     */
    public void setResultMaskColor(int color) {
        if (color == 0) {
            return;
        }
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
        if (lightOnDrawable == 0) {
            return;
        }
        mLightOn = BitmapUtil.getBitmap(getContext(), lightOnDrawable);
    }

    /**
     * 设置手电筒关闭时的图标
     */
    public void setFlashLightOffDrawable(int lightOffDrawable) {
        if (lightOffDrawable == 0) {
            return;
        }
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

    /**
     * 关闭闪光灯
     */
    void turnOffLight() {
        isDark = false;
        isLightOn = false;
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

    public Point getScanCenter() {
        return new Point(mFramingRect.centerX(), mFramingRect.centerY());
    }

    public Rect getScanBoxRect() {
        return mFramingRect;
    }

    public int getScanBoxSize() {
        return Math.min(mFramingRect.width(), mFramingRect.height());
    }

    public interface ScanBoxClickListener {
        void onFlashLightClick();
    }
}
