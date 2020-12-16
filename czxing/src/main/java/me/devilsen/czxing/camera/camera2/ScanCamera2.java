package me.devilsen.czxing.camera.camera2;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import me.devilsen.czxing.camera.CameraSize;
import me.devilsen.czxing.camera.ScanCamera;
import me.devilsen.czxing.compat.ActivityCompat;
import me.devilsen.czxing.util.BarCodeUtil;
import me.devilsen.czxing.util.Camera2Helper;
import me.devilsen.czxing.view.AutoFitSurfaceView;

/**
 * Camera2 API Camera
 *
 * @author : dongSen
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScanCamera2 extends ScanCamera {

    /** Maximum number of images that will be held in the reader's buffer */
    private static final int IMAGE_BUFFER_SIZE = 2;
    private static final String FOCUS_TAG = "focus_tag";

    private final CameraManager mCameraManager;
    private String mCameraId;
    private CameraCharacteristics mCharacteristics;

    /** [HandlerThread] where all camera operations run */
    private final HandlerThread mCameraThread = new HandlerThread("CameraThread");
    /** [Handler] corresponding to [cameraThread] */
    private Handler mCameraHandler;
    /** Readers used as buffers for camera still shots */
    private ImageReader mImageReader;
    /** Camera2 */
    private CameraDevice mCamera;
    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private CameraCaptureSession mCaptureSession;
    /** Builder for capture request */
    private CaptureRequest.Builder mCaptureBuilder;
    /** Focus Array size */
    private Rect mSensorArraySize;

    /** Is support flash light */
    private boolean isFlashSupported;
    /** AF stands for AutoFocus */
    private boolean isAFSupported;
    /** AE stands for AutoExposure */
    private boolean isAESupported;
    /** Manual focus is engaging */
    private boolean mManualFocusEngaged;

    private float mZoomLevel;
    private float mMaxZoomLevel;
    private boolean mCameraOping;
    private boolean mCameraClosed;
    private Size mPreviewSize;

    public ScanCamera2(Context context, AutoFitSurfaceView surfaceView) {
        super(context, surfaceView);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onCreate() {
        if (mCamera != null || mCameraOping) return;
        setUpCameraId();
        setupCallback();
    }

    @Override
    public void onResume() {
        mCameraClosed = false;
        mSensorController.onStart();
    }

    @Override
    public void onPause() {
        if (mCameraClosed) return;
        mCameraClosed = true;

        mIsFlashLighting = false;
        closeFlashlight();
        mSensorController.onStop();
        try {
            mCamera.close();
        } catch (Throwable throwable) {
            BarCodeUtil.e("Error closing camera" + throwable);
            mCameraClosed = false;
        }
    }

    @Override
    public void onDestroy() {
        synchronized (ScanCamera2.class) {
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            mCameraHandler.removeCallbacksAndMessages(null);
            mCameraThread.quitSafely();
            if (mImageReader != null) {
                mImageReader.close();
                mImageReader = null;
            }
        }
    }

    /**
     * 1. get the back camera id
     */
    private void setUpCameraId() {
        try {
            mCameraOping = true;
            String[] cameraIdList = mCameraManager.getCameraIdList();

            for (String cameraId : cameraIdList) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraDirection == null) {
                    continue;
                }
                if (cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                this.mCharacteristics = characteristics;
                this.mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 2. add holder callback, auto process open camera when resume
     */
    private void setupCallback() {
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                mPreviewSize = CameraSize.getPreviewOutputSize(mSurfaceView.getDisplay(), mCharacteristics, SurfaceHolder.class);
                BarCodeUtil.d("View finder size: " + mSurfaceView.getWidth() + " x " + mSurfaceView.getHeight());
                BarCodeUtil.d("Selected preview size: " + mPreviewSize);
                mSurfaceView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                mSurfaceView.post(new Runnable() {
                    @Override
                    public void run() {
                        openCamera();
                    }
                });
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

    /**
     * 3. open camera
     */
    public void openCamera() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (!mCameraThread.isAlive()) {
            mCameraThread.start();
            mCameraHandler = new Handler(mCameraThread.getLooper());
        }

        try {
            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mCamera = camera;
                    initializeCamera();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    BarCodeUtil.w("Disconnected Camera finish Activity");
                    if (mContext instanceof Activity) {
                        ((Activity) mContext).finish();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    BarCodeUtil.e("Open camera error, code = " + error);
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 4. initialize camera
     */
    private void initializeCamera() {
        int pixelFormat = ImageFormat.YUV_420_888;
        Size[] outputSizes = mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(pixelFormat);
        Boolean available = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        isFlashSupported = available == null ? false : available;
        Integer afRegion = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        isAFSupported = afRegion != null && afRegion >= 1;
        Integer aeState = mCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        isAESupported = aeState != null && aeState >= 1;
        mSensorArraySize = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

//        Size maxSize = new Size(0, 0);
//        int area = 0;
//        for (Size size : outputSizes) {
//            int areaTemp = size.getWidth() * size.getHeight();
//            if (areaTemp > area) {
//                area = areaTemp;
//                maxSize = size;
//            }
//        }
        BarCodeUtil.d("image reader-preview size: width = " + mPreviewSize.getWidth() + " height = " + mPreviewSize.getHeight());
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), pixelFormat, IMAGE_BUFFER_SIZE);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (mScanCallback != null) {
//                    BarCodeUtil.d("image reader: width  = " + mImageReader.getWidth() + " height = " + mImageReader.getHeight());
                    mScanCallback.onPreviewFrame(Camera2Helper.readYuv(reader), mImageReader.getWidth(), mImageReader.getHeight());
                }
            }
        }, null);

        List<Surface> targets = new ArrayList<>(2);
        targets.add(mSurfaceView.getHolder().getSurface());
        targets.add(mImageReader.getSurface());

        createCaptureSession(mCamera, targets, mCameraHandler);
    }

    /**
     * 5. crate session
     */
    private void createCaptureSession(final CameraDevice device, List<Surface> targets, Handler cameraHandler) {
        try {
            device.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    ScanCamera2.this.mCaptureSession = session;

                    createCapture(device, session);
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    BarCodeUtil.e("Camera " + device.getId() + " session configuration failed");
                }
            }, cameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 6. ready to capture
     */
    private void createCapture(CameraDevice device, CameraCaptureSession session) {
        try {
            mCaptureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureBuilder.addTarget(mImageReader.getSurface());
            mCaptureBuilder.addTarget(mSurfaceView.getHolder().getSurface());
            mCameraOping = false;

            session.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openFlashlight() {
        if (!isFlashSupported || mIsFlashLighting) {
            return;
        }
        try {
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
            mIsFlashLighting = true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closeFlashlight() {
        if (!isFlashSupported || !mIsFlashLighting) {
            return;
        }
        try {
            mCaptureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
            mIsFlashLighting = false;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void focus(float focusPointX, float focusPointY) {
        try {
            BarCodeUtil.d("Focus x = " + focusPointX + " y = " + focusPointY);
            setFocusArea(focusPointX, focusPointY);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setFocusArea(float focusPointX, float focusPointY) throws CameraAccessException {
        if (focusPointX < 0 || focusPointY < 0) return;
        if (mManualFocusEngaged) return;

        float y = focusPointX;
        float x = focusPointY;

        if (mSensorArraySize != null) {
            y = (int) ((focusPointX / mSurfaceView.getWidth()) * (float) mSensorArraySize.height());
            x = (int) ((focusPointY / mSurfaceView.getHeight()) * (float) mSensorArraySize.width());
        }

        final int halfTouchLength = 150;
        MeteringRectangle focusArea = new MeteringRectangle(
                (int) Math.max(x - halfTouchLength, 0),
                (int) Math.max(y - halfTouchLength, 0),
                halfTouchLength * 2,
                halfTouchLength * 2,
                MeteringRectangle.METERING_WEIGHT_MAX - 1);

        mCaptureSession.stopRepeating(); // Destroy current session
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
        mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
        mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, mCameraHandler); //Set all settings for once

        if (isAESupported) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{focusArea});
        }
        if (isAFSupported) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusArea});
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
        }

        mCaptureBuilder.setTag(FOCUS_TAG); //it will be checked inside mCaptureCallback
        mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, mCameraHandler);

        mManualFocusEngaged = true;
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            mManualFocusEngaged = false;

            if (FOCUS_TAG.equals(request.getTag())) {
                //the focus trigger is complete -
                //resume repeating (preview surface will get frames), clear AF trigger
                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                mCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, null);// As documentation says AF_trigger can be null in some device
                try {
                    session.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            mManualFocusEngaged = false;
        }

    };

    @Override
    public boolean isZoomSupported() {
        return mSensorArraySize != null;
    }

    @Override
    public float zoom(float zoomLevel) {
        if (mCharacteristics == null || mCaptureBuilder == null || mCaptureSession == null || mCameraClosed) {
            return 0;
        }
        try {
            float maxZoom = getMaxZoom();
            Rect m = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (zoomLevel < 0) {
                return 0;
            }
            if (zoomLevel > maxZoom) {
                return (int) maxZoom;
            }
            if (zoomLevel < mZoomLevel) {
                mZoomOutFlag = true;
            }
            mZoomLevel = zoomLevel;

            int minW = (int) (m.width() / maxZoom);
            int minH = (int) (m.height() / maxZoom);
            int difW = m.width() - minW;
            int difH = m.height() - minH;
            int cropW = (int) (difW / 100 * zoomLevel);
            int cropH = (int) (difH / 100 * zoomLevel);
            cropW -= cropW & 3;
            cropH -= cropH & 3;
            Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);

            mCaptureBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return zoomLevel;
    }

    @Override
    public float getMaxZoom() {
        if (mMaxZoomLevel == 0) {
            mMaxZoomLevel = (mCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 7;
        }
        return mMaxZoomLevel;
    }

    @Override
    public float getZoom() {
        return mZoomLevel;
    }

    @Override
    public void doubleTap() {
        mZoomLevel = mZoomLevel + 5;
        zoom(mZoomLevel);
    }

    @Override
    public void touchZoom(float distance) {
        if (distance > 0) {
            mZoomLevel++;
        } else {
            mZoomLevel--;
        }
        zoom(mZoomLevel);
    }
}
