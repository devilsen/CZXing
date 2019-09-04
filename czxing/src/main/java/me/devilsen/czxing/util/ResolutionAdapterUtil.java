package me.devilsen.czxing.util;

import android.content.Context;

import me.devilsen.czxing.camera.CameraUtil;

/**
 * desc : 分辨率转换util
 * date : 2019-08-20 17:07
 *
 * @author : dongSen
 */
public class ResolutionAdapterUtil {

    private int resolutionWidth;
    private int resolutionHeight;

    private int cameraWidth;
    private int cameraHeight;

    private float ratioWidth;
    private float ratioHeight;

    public void setResolutionSize(int resolutionWidth, int resolutionHeight) {
        this.resolutionWidth = resolutionWidth;
        this.resolutionHeight = resolutionHeight;
        setRatio();
    }

    public void setCameraSize(boolean portrait, int cameraWidth, int cameraHeight) {
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        if (portrait) {
            int temp = this.cameraWidth;
            this.cameraWidth = this.cameraHeight;
            this.cameraHeight = temp;
        }
        setRatio();
    }

    private void setRatio() {
        if (ratioWidth == 0 && resolutionWidth != 0 && cameraWidth != 0) {
            ratioWidth = 1.0f * cameraWidth / resolutionWidth;
        }
        if (ratioHeight == 0 && resolutionHeight != 0 && cameraHeight != 0) {
            ratioHeight = 1.0f * cameraHeight / resolutionHeight;
        }
    }

    public int getAdapterWidth(int width) {
        if (ratioWidth != 0) {
            return (int) (ratioWidth * width);
        }
        return width;
    }

    public int getAdapterHeight(int height) {
        if (ratioHeight != 0) {
            return (int) (ratioHeight * height);
        }
        return height;
    }
}
