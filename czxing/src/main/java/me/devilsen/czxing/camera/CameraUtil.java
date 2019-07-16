package me.devilsen.czxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import me.devilsen.czxing.util.BarCodeUtil;

/**
 * @author : dongSen
 * date : 2019-06-29 14:56
 * desc : 相机工具
 */
public class CameraUtil {

    /**
     * 是否为竖屏
     */
    public static boolean isPortrait(Context context) {
        Point screenResolution = getScreenResolution(context);
        return screenResolution.y > screenResolution.x;
    }

    static Point getScreenResolution(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point screenResolution = new Point();
        display.getSize(screenResolution);
        return screenResolution;
    }

    /**
     * 计算手指间距
     */
    static float calculateFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 计算对焦和测光区域
     *
     * @param coefficient        比率
     * @param originFocusCenterX 对焦中心点X
     * @param originFocusCenterY 对焦中心点Y
     * @param originFocusWidth   对焦宽度
     * @param originFocusHeight  对焦高度
     * @param previewViewWidth   预览宽度
     * @param previewViewHeight  预览高度
     *                           <p>
     *                           https://www.cnblogs.com/panxiaochun/p/5802814.html
     */
    static Rect calculateFocusMeteringArea(float coefficient,
                                           float originFocusCenterX, float originFocusCenterY,
                                           int originFocusWidth, int originFocusHeight,
                                           int previewViewWidth, int previewViewHeight) {

        int halfFocusAreaWidth = (int) (originFocusWidth * coefficient / 2);
        int halfFocusAreaHeight = (int) (originFocusHeight * coefficient / 2);

        int centerX = (int) (originFocusCenterX / previewViewWidth * 2000 - 1000);
        int centerY = (int) (originFocusCenterY / previewViewHeight * 2000 - 1000);

        RectF rectF = new RectF(clamp(centerX - halfFocusAreaWidth, -1000, 1000),
                clamp(centerY - halfFocusAreaHeight, -1000, 1000),
                clamp(centerX + halfFocusAreaWidth, -1000, 1000),
                clamp(centerY + halfFocusAreaHeight, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
    }

    static void printRect(String prefix, Rect rect) {
        BarCodeUtil.d(prefix + " centerX：" + rect.centerX() + " centerY：" + rect.centerY() + " width：" + rect.width() + " height：" + rect.height()
                + " rectHalfWidth：" + rect.width() / 2 + " rectHalfHeight：" + rect.height() / 2
                + " left：" + rect.left + " top：" + rect.top + " right：" + rect.right + " bottom：" + rect.bottom);
    }

    static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
    }

    static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue, context.getResources().getDisplayMetrics());
    }


}
