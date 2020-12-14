package me.devilsen.czxing.camera;

import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Size;
import android.view.Display;

import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.Comparator;

/**
 * desc : 获取合适的分辨率
 * date : 12/7/20 4:56 PM
 *
 * @author : dongSen
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraSize {

    /**
     * Standard High Definition size for pictures and video
     */
    private static final SmartSize SIZE_1080P = new SmartSize(1920, 1080);

    static class SmartSize {
        Size size;
        int width;
        int height;

        public SmartSize(Size size) {
            this.size = size;
            width = size.getWidth();
            height = size.getHeight();
        }

        public SmartSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        int getLong() {
            return Math.max(width, height);
        }

        int getShort() {
            return Math.min(width, height);
        }

        @Override
        public String toString() {
            return "SmartSize{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

    /**
     * Returns a [SmartSize] object for the given [Display]
     */
    public static SmartSize getDisplaySmartSize(Display display) {
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        return new SmartSize(outPoint.x, outPoint.y);
    }

    /**
     * Returns the largest available PREVIEW size. For more information, see:
     * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
     * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
     */
    public static <T> Size getPreviewOutputSize(
            Display display,
            CameraCharacteristics characteristics,
            Class<T> targetClass) {

        // Find which is smaller: screen or 1080p
        SmartSize screenSize = getDisplaySmartSize(display);
        boolean hdScreen = screenSize.getLong() >= SIZE_1080P.getLong() || screenSize.getShort() >= SIZE_1080P.getShort();
        SmartSize maxSize;
        if (hdScreen) {
            maxSize = SIZE_1080P;
        } else {
            maxSize = screenSize;
        }

        // If image format is provided, use it to determine supported sizes; else use target class
        StreamConfigurationMap config = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        StreamConfigurationMap.isOutputSupportedFor(targetClass);
        Size[] allSizes = config.getOutputSizes(targetClass);

        // Get available sizes and sort them by area from largest to smallest
        Arrays.sort(allSizes, new Comparator<Size>() {
            @Override
            public int compare(Size o1, Size o2) {
                return o1.getHeight() * o1.getWidth() - o2.getHeight() * o2.getWidth();
            }
        });

        for (int i = allSizes.length - 1; i >= 0; i--) {
            SmartSize it = new SmartSize(allSizes[i]);
            if (it.getLong() <= maxSize.getLong() && it.getShort() <= maxSize.getShort()) {
                return it.size;
            }
        }

        return SIZE_1080P.size;
    }

}
