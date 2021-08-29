package me.devilsen.czxing.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import me.devilsen.czxing.R;
import me.devilsen.czxing.util.BarCodeUtil;

public class PointView extends View {

    private Context mContext;
    private Paint mPaint;
    private int mColor;
    private int mWhiteColor;
    private int mBigRadius = 15;
    private int mLittleRadius = 12;

    private int mX;
    private int mY;

    public PointView(Context context) {
        this(context, null);
    }

    public PointView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PointView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        mBigRadius = BarCodeUtil.dp2px(context, 15);
        mLittleRadius = BarCodeUtil.dp2px(context, 10);
        mColor = context.getResources().getColor(R.color.czxing_point_green);
        mWhiteColor = context.getResources().getColor(R.color.czxing_point_white);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            mX = (right - left) / 2;
            mY = (bottom - top) / 2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setColor(mWhiteColor);
        canvas.drawCircle(mX, mY, mBigRadius, mPaint);

        mPaint.setColor(mColor);
        canvas.drawCircle(mX, mY, mLittleRadius, mPaint);
    }

    public void setColor(@ColorRes int color) {
        if (color > 0 )
        mColor = mContext.getResources().getColor(color);
    }
}
