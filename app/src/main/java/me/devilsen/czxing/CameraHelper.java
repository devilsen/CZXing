package me.devilsen.czxing;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.List;

public class CameraHelper implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final String TAG = "CameraHelper";
    private Activity mActivity;
    private int mHeight;
    private int mWidth;
    private int mCameraId;
    private Camera mCamera;
    private byte[] buffer;
    private SurfaceHolder mSurfaceHolder;
    private Camera.PreviewCallback mPreviewCallback;
    private int mRotation;
    private OnChangedSizeListener mOnChangedSizeListener;
    byte[] bytes;

    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraId = cameraId;
        mWidth = width;
        mHeight = height;
    }

    public void switchCamera() {
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    public void openCamera() {
        stopPreview();
        startPreview();
    }

    private void stopPreview() {
        if (mCamera != null) {
            //预览数据回调接口
            mCamera.setPreviewCallback(null);
            //停止预览
            mCamera.stopPreview();
            //释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    private void startPreview() {
        try {
            //获得camera对象
            mCamera = Camera.open(mCameraId);
            //配置camera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            //设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            //这是摄像头宽、高
            setPreviewSize(parameters);
            // 设置摄像头 图像传感器的角度、方向
            setPreviewOrientation(parameters);
            mCamera.setParameters(parameters);
            buffer = new byte[mWidth * mHeight * 3 / 2];
            bytes = new byte[buffer.length];
            //数据缓存区
            mCamera.addCallbackBuffer(buffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            //设置预览画面
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //显示画面        会调用  编码    横着的
    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, info);
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                mOnChangedSizeListener.onChanged(mHeight, mWidth);
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                degrees = 90;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                degrees = 270;
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        //设置角度
        mCamera.setDisplayOrientation(result);
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        //获取摄像头支持的宽、高
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "Surface大小 " + mWidth + "x" + mHeight);
        Log.d(TAG, "支持 " + size.width + "x" + size.height);
        //选择一个与设置的差距最小的支持分辨率
        // 10x10 20x20 30x30
        // 12x12
        int m = Math.abs(size.height * size.width - mWidth * mHeight);
        supportedPreviewSizes.remove(0);
        //遍历
        for (Camera.Size next : supportedPreviewSizes) {
            Log.d(TAG, "支持 " + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - mWidth * mHeight);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);
        Log.d(TAG, "设置预览分辨率 width:" + size.width + " height:" + size.height);
    }


    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        mPreviewCallback = previewCallback;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //释放摄像头
        stopPreview();
        //开启摄像头
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        switch (mRotation) {
            case Surface.ROTATION_0:
                rotation90(data);
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                break;
            case Surface.ROTATION_270:// 横屏 头部在右边
                break;
        }
        // data数据依然是倒的
        mPreviewCallback.onPreviewFrame(bytes, camera);
        camera.addCallbackBuffer(buffer);
    }

    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = mWidth * mHeight;
        //u和v
        int uvHeight = mHeight / 2;
        //后置摄像头顺时针旋转90度
        if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //将y的数据旋转之后 放入新的byte数组
            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    bytes[index++] = data[mWidth * j + i];
                }
            }

            //每次处理两个数据
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    bytes[index++] = data[ySize + mWidth * j + i];
                    // u
                    bytes[index++] = data[ySize + mWidth * j + i + 1];
                }
            }
        } else {
            //逆时针旋转90度
            for (int i = 0; i < mWidth; i++) {
                int nPos = mWidth - 1;
                for (int j = 0; j < mHeight; j++) {
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
            //u v
            for (int i = 0; i < mWidth; i += 2) {
                int nPos = ySize + mWidth - 1;
                for (int j = 0; j < uvHeight; j++) {
                    bytes[index++] = data[nPos - i - 1];
                    bytes[index++] = data[nPos - i];
                    nPos += mWidth;
                }
            }
        }
    }


    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        mOnChangedSizeListener = listener;
    }

    public void release() {
        mSurfaceHolder.removeCallback(this);
        stopPreview();
    }

    public interface OnChangedSizeListener {
        void onChanged(int w, int h);
    }
}
