package me.devilsen.czxing.camera;

/**
 * @author : dongSen
 * date : 2019-06-29 14:31
 * desc :
 */
public interface ICamera {

    void startCameraPreview();

    void stopCameraPreview();

    void openFlashlight();

    void closeFlashlight();
}
