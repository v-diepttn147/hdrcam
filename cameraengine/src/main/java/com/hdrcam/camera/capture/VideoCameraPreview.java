package com.hdrcam.camera.capture;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import com.hdrcam.camera.render.OverlayView;
import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ExImageFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 * Created by oleg on 11/2/17.
 */

public class VideoCameraPreview {
    private static final String TAG = VideoCameraPreview.class.toString();

    private PreviewImageListener mPreviewImageListener;
//    private SavedImageListener mSavedListenerRaw;
    private SavedImageListener mSavedListenerJpeg;
    private FrameHandler mFrameHandler;
    private Context mContext;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mImPreviewReader;
//    private ImageReader mImSaveReaderRaw;
    private ImageReader mImSaveReaderJpeg;
    private boolean mIsFrontCam = false;


    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Integer mSensorOrientation;
    private int mPhoneRotation;
    private List<Size> mOutputSizes = new ArrayList<>();

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private CameraCaptureSession.CaptureCallback mTakePictureCallback;
    private CaptureRequest mTakePictureRequest;

    public Size getPreviewSize() {
        return mPreviewSize;
    }

    public Size getSaveSize() {
        return mSaveSizeJpeg;
    }

    public String getCameraId() {
        return mCameraId;
    }

    public boolean getCameraFacing() {
        return mCameraId.equals(Constants.FRONT_CAM);

    }

    private Size mPreviewSize;
    private Size mSaveSizeRaw;
    private Size mSaveSizeJpeg;
    private String resolution = "AUTO";

    private ImageReader.OnImageAvailableListener mListener;
    private Constants.CAM_MODE mMode;

    private CameraManager mCameraManager;
    private CameraCharacteristics mCameraCharacteristics;
    private Rect activeArraySizeRect;
    private Boolean mFaceDetectionEnabled;
    private int mCamSupportedPreviewFormat;
    private int mCamSupportedSaveFormat;

    public Integer getSensorOrientation() {
        return mSensorOrientation;
    }

    public Boolean getmFaceDetectionEnabled() {
        return mFaceDetectionEnabled;
    }

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    public VideoCameraPreview(Context context, Constants.CAM_MODE mode, String cameraId,
                              int userPreviewFormat, int userSaveFormat, Boolean faceDetectionEnabled) {

        mContext = context;
        mCameraId = cameraId;
        //    mPreviewSize = previewSize;

        if (userPreviewFormat == ExImageFormat.NV12 || userPreviewFormat == ExImageFormat.NV21)
            mCamSupportedPreviewFormat = ImageFormat.YUV_420_888;
        else mCamSupportedPreviewFormat = userPreviewFormat;
        Log.d(TAG, "Preview format is: " + mCamSupportedPreviewFormat);
        if (userSaveFormat == ExImageFormat.NV12 || userSaveFormat == ExImageFormat.NV21)
            mCamSupportedSaveFormat = ImageFormat.YUV_420_888;
        else if (userSaveFormat == ExImageFormat.CONVERTED_NV21)
            mCamSupportedSaveFormat = ImageFormat.JPEG;
        else
            mCamSupportedSaveFormat = userSaveFormat;

        Log.d(TAG, "Save format is: " + mCamSupportedSaveFormat);

        if (mCameraId.equals("2")) mFaceDetectionEnabled = false;
        else mFaceDetectionEnabled = faceDetectionEnabled;

        mPreviewImageListener = new PreviewImageListener((FrameHandler) context, mode, mCameraId, userPreviewFormat);
//        mSavedListenerRaw = new SavedImageListener((FrameHandler) context, mContext, mode,
//                mCameraId, ImageFormat.RAW_SENSOR);
        mSavedListenerJpeg = new SavedImageListener((FrameHandler) context, mContext, mode,
                mCameraId, ExImageFormat.CONVERTED_NV21);

        getCameraCharacteristics(mCameraId);
    }

    public void setCameraId(String id) {
        mCameraId = id;
    }

    public void setFrameHandler(FrameHandler frameHandler) {
        this.mFrameHandler = frameHandler;
    }

    public void startCamera() {
        startBackgroundThread();
        openCamera(mPreviewSize.getWidth(), mPreviewSize.getHeight());
    }

    public void stopCamera() {
/*        mImPreviewReader.setOnImageAvailableListener(null, null);
        mImSaveReader.setOnImageAvailableListener(null, null);*/

        try {
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeCamera();
        stopBackgroundThread();
    }

    public void changeSize(Size size) {
        mPreviewSize = size;

        stopCamera();
        startCamera();
    }

    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity(mContext);

        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        mImPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(),
                mPreviewSize.getHeight(), mCamSupportedPreviewFormat, 2);
        mImPreviewReader.setOnImageAvailableListener(mPreviewImageListener, mBackgroundHandler);

//        mImSaveReaderRaw = ImageReader.newInstance(mSaveSizeRaw.getWidth(),
//                mSaveSizeRaw.getHeight(),
//                ImageFormat.RAW_SENSOR, Constants.burstSize);
//        mImSaveReaderRaw.setOnImageAvailableListener(
//                mSavedListenerRaw,
//                mBackgroundHandler);

        mImSaveReaderJpeg = ImageReader.newInstance(mSaveSizeJpeg.getWidth(),
                mSaveSizeJpeg.getHeight(),
                ImageFormat.JPEG, Constants.burstSize);
        mImSaveReaderJpeg.setOnImageAvailableListener(
                mSavedListenerJpeg,
                mBackgroundHandler);

        try {
            mCameraCharacteristics
                    = mCameraManager.getCameraCharacteristics(mCameraId);
            activeArraySizeRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

//            mSavedListenerRaw.setCameraCharacteristics(mCameraCharacteristics);
//            mSavedListenerRaw.setBurstSize(Constants.burstSize);

            mSavedListenerJpeg.setCameraCharacteristics(mCameraCharacteristics);
            mSavedListenerJpeg.setBurstSize(Constants.burstSize);

           /* int[] aeModes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            Range<Long> time =
                    mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);*/
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the camera specified by {@link VideoCameraPreview#}.
     */
    public void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Log.i(TAG, "openCamera");
        setUpCameraOutputs(width, height);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access the camera." + e.toString());
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImPreviewReader) {
                mImPreviewReader.close();
                mImPreviewReader = null;
            }

//            if (null != mImSaveReaderRaw) {
//                mImSaveReaderRaw.close();
//                mImSaveReaderRaw = null;
//            }
            if (null != mImSaveReaderJpeg) {
                mImSaveReaderJpeg.close();
                mImSaveReaderJpeg = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }

        Log.i(TAG, "closeCamera" + mCameraId);
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();

            mBackgroundThread = null;
            mBackgroundHandler = null;

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return true if the given array contains the given integer.
     *
     * @param modes array to check.
     * @param mode  integer to get for.
     * @return true if the array contains the given integer, otherwise false.
     */
    private static boolean contains(int[] modes, int mode) {
        if (modes == null) {
            return false;
        }
        for (int i : modes) {
            if (i == mode) {
                return true;
            }
        }
        return false;
    }

    private CaptureRequest createCaptureRequest() {
        if (null == mCameraDevice) return null;
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(mImPreviewReader.getSurface());

            // Enable auto-magical 3A run by camera device
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE,
                    CaptureRequest.CONTROL_MODE_AUTO);

            Float minFocusDist =
                    mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

            // If MINIMUM_FOCUS_DISTANCE is 0, lens is fixed-focus and we need to skip the AF run.
            boolean noAFRun = (minFocusDist == null || minFocusDist == 0);

            if (!noAFRun) {
                // If there is a "continuous picture" mode available, use it, otherwise default to AUTO.
                if (contains(mCameraCharacteristics.get(
                        CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES),
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                } else {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_AUTO);
                }
            }

            // If there is an auto-magical flash control mode available, use it, otherwise default to
            // the "on" mode, which is guaranteed to always be available.
            // We won't use this for now
            /*
            if (contains(mCharacteristics.get(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)) {
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            } else {
                builder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
            }
            */

            // Manually set Exposure time and ISO or using AE mode depends on isAutoexposure
            // But still disable flash light
            if (Constants.isAutoexposure) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            }
            else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
                mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, 1000000000L / Constants.allExpFrac[Constants.expTimeVal]);
                mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, Constants.ISOCustomVal); //Constants.allISO[Constants.ISOVal]);
            }
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

            // If there is an auto-magical white balance control mode available, use it.
            if (contains(mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES),
                    CaptureRequest.CONTROL_AWB_MODE_AUTO)) {
                // Allow AWB to run auto-magically if this device supports this
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                        CaptureRequest.CONTROL_AWB_MODE_AUTO);
            }

            /*
            if (mFaceDetectionEnabled)
                mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                        CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE);
            */

            // builder.addTarget(mImageSaveReader.getSurface());
            return mPreviewRequestBuilder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity(mContext);
            if (null != activity) {
                activity.finish();
            }
        }
    };

    public void createCaptureSession() {
        try {
            if (null == mCameraDevice || null == mImPreviewReader) return;
           /* mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    sessionStateCallback, mBackgroundHandler);*/
            mCameraDevice.createCaptureSession(Arrays.asList(mImPreviewReader.getSurface(),
                    mImSaveReaderJpeg.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            try {
                                // CaptureRequest captureRequest = createCaptureRequest();
                                // mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                //        CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);
                                mPreviewRequest = createCaptureRequest();

                                if (mPreviewRequest != null) {
                                    session.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
                                } else {
                                    Log.e(TAG, "captureRequest is null");
                                }
                            } catch (CameraAccessException | IllegalStateException e) {
                                Log.e(TAG, "onConfigured " + e.toString());
                            }
                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = session;
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e(TAG, "onConfigureFailed");
                        }
                    }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession " + e.toString());
        }
    }

    private Size chooseOptimalSize(Size[] sizes, int expectedWidth, int expectedHeight) {
        //  String model = Build.MODEL;

        int diff = Integer.MAX_VALUE;
        Size optimalSize = new Size(0, 0);
        for (Size size : sizes) {
            int cDiff = Math.abs(size.getWidth() - expectedWidth) + Math.abs(size.getHeight() - expectedHeight);
            if (cDiff < diff) {
                diff = cDiff;
                optimalSize = size;
            }
        }
        Log.d(TAG, String.format("Optimal size is %d %d", optimalSize.getWidth(),
                optimalSize.getHeight()));
        return optimalSize;
    }

    private void getCameraCharacteristics(String cameraId) {

        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap streamConfigs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (Constants.userDefinedSaveSize != null && !mCameraId.equals(Constants.DUAL_SUB_CAM)) {

                mSaveSizeJpeg = Constants.userDefinedSaveSize;
                Log.d(TAG, String.format("image_size_save manually is (%d,%d)",
                        mSaveSizeJpeg.getWidth(),
                        mSaveSizeJpeg.getHeight()));
            } else {
                if (Build.MODEL.contains("Live") && mCameraId.equals(Constants.FRONT_CAM)) {
                    mSaveSizeJpeg = new Size(3840, 2160);

                    Log.d(TAG, String.format("image_size_save for Live is (%d,%d)",
                            mSaveSizeJpeg.getWidth(),
                            mSaveSizeJpeg.getHeight()));
                } else {
                    mSaveSizeJpeg =
                            chooseOptimalSize(streamConfigs.getOutputSizes(ImageFormat.JPEG), 4000, 3000);
                    mSaveSizeRaw =
                            chooseOptimalSize(streamConfigs.getOutputSizes(ImageFormat.RAW_SENSOR), 4000, 3000);
                    Log.d(TAG, String.format("image_size_save for raw is (%d,%d)",
                            mSaveSizeRaw.getWidth(),
                            mSaveSizeRaw.getHeight()));
                    Log.d(TAG, String.format("image_size_save for jpeg is (%d,%d)",
                            mSaveSizeJpeg.getWidth(),
                            mSaveSizeJpeg.getHeight()));
                }
            }

            if (Constants.userDefinedPreviewSize !=null && !mCameraId.equals(Constants.DUAL_SUB_CAM)) {
                mPreviewSize = Constants.userDefinedPreviewSize;
                Log.d(TAG, String.format("image_size_save manually is (%d,%d)",
                        mPreviewSize.getWidth(),
                        mPreviewSize.getHeight()));
            }
            else mPreviewSize =
                    chooseOptimalSize(streamConfigs.getOutputSizes(mCamSupportedPreviewFormat), 1280, 720);
            Log.d(TAG, String.format("image_size_preview for Other is (%d,%d)",
                    mPreviewSize.getWidth(),
                    mPreviewSize.getHeight()));
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null) {
                mIsFrontCam = (facing == CameraCharacteristics.LENS_FACING_FRONT);
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access the camera." + e.toString());
        }
    }

    public Size getCameraCharacteristics(int w, int h) {
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                StreamConfigurationMap streamConfigs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (streamConfigs != null) {
                    mOutputSizes = Arrays.asList(streamConfigs.getOutputSizes(SurfaceTexture.class));
                }
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                //mCameraId = cameraId;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access the camera." + e.toString());
        }

        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (mOutputSizes == null)
            return null;

        Size optimalSize = null;

        // Start with max value and refine as we iterate over available preview sizes. This is the
        // minimum difference between view and camera height.
        double minDiff = Double.MAX_VALUE;

        double diffSize = Double.MAX_VALUE;
        double curDiffSize;

        for (Size size : mOutputSizes) {
            curDiffSize = Math.abs((double) w - (double) size.getHeight());
            if (curDiffSize < diffSize) {
                diffSize = curDiffSize;
                optimalSize = size;
            }
        }

        return optimalSize;
    }

    //////////////////////////addded////////////////////////////////////////////////
    public Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }

        return null;
    }


    public void takePicture(int phoneRotation) {
        mPhoneRotation = phoneRotation;
        captureStillPicture();
    }

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    public boolean prepareTakePicture(int phoneRotation) {
        try {
            int dir = mCameraId.equals(Constants.FRONT_CAM) ? -1 : 1;
            int orientation = (mSensorOrientation + dir * phoneRotation) % 360;

            Log.d("ABCD", String.format("orientation %d", orientation));

//            mSavedListenerRaw.setRotation(orientation);
//            mSavedListenerRaw.setBackgroundHandler(mBackgroundHandler);
            mSavedListenerJpeg.setRotation(orientation);
            mSavedListenerJpeg.setBackgroundHandler(mBackgroundHandler);
            Log.d(TAG, String.format("Thread Cam#%s tid%s - preparing", mCameraId,
                    Thread.currentThread().getId()));
            final Activity activity = getActivity(mContext);
            if (activity == null || null == mCameraDevice) {
                return false;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(mImSaveReaderRaw.getSurface());
            captureBuilder.addTarget(mImSaveReaderJpeg.getSurface());
            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

/*            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 2);

            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)863370925);*/
         /*   captureBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE);*/

//            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
//            captureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CameraMetadata.CONTROL_SCENE_MODE_FACE_PRIORITY);

            // setAutoFlash(captureBuilder);
            if (Constants.autoWhiteBalanceOn) {
                captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);
            }

            // Orientation
            // captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);
//            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 5);
//            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, (long)525313925);
            mTakePictureCallback
                    = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull CaptureResult partialResult){
                    Log.d("ABCD", String.format("capture onProgress"));
//                    mSavedListenerRaw.setCaptureResult(partialResult);
//                    mSavedListenerJpeg.setCaptureResult(partialResult);
                }
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    // TODO: Check here if we need to sync
                    // if (mCamSupportedSaveFormat == ImageFormat.RAW_SENSOR) {
                    //     synchronized (Constants.saveLock) {
                    //         Constants.rawSavedImData.setCaptureResult(result);
                    //         if (Constants.rawSavedImData.getRawImage() != null) {
                    //             Log.d("rawsave", "dosave from on capture");
                    //             mSavedListener.mSaveHandler.doSave(Constants.rawSavedImData.getRawImage(),
                    //                     Constants.rawSavedImData.getCameraCharacteristics(),
                    //                     Constants.rawSavedImData.getCaptureResult());
                    //         }

                    //     }
                    // }
                    Log.d("ABCD", String.format("start save %d", mCamSupportedSaveFormat));
//                    mSavedListenerRaw.setCaptureResult(result);
                    mSavedListenerJpeg.setCaptureResult(result);
                    unlockFocus();
                }
                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureFailure failure) {
                    Log.d("ABCD", String.format("capture Failed "));
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();

            mTakePictureRequest = captureBuilder.build();
            return true;
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {link lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            //   final Activity activity = getActivity(mContext);
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            captureBuilder.addTarget(mImSaveReaderRaw.getSurface());
            captureBuilder.addTarget(mImSaveReaderJpeg.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO);

            // Manually set exposure time and ISO
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

            // A heuristic to set exposure time
            Long exposureTime = 1000000000L / Constants.allExpFrac[Constants.expTimeVal];
            Integer isoValue = Constants.allISO[Constants.ISOVal];
            {
                // TODO: Check if we should underexpose

                // Exposure time should not be too small, we cap at 1/200 (5ms = 5000000ns)
                // Currently ignore when in this capture app
                // exposureTime = Math.max(exposureTime, 5000000);
            }
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue);

            int dir = mCameraId.equals(Constants.FRONT_CAM) ? -1 : 1;
            int orientation = (mSensorOrientation + dir * mPhoneRotation) % 360;
            Log.d(TAG, String.format("captureStillPicture rotation %d", mPhoneRotation));
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);

//            List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
//            for (int i = 0; i < Constants.burstSize; i++) {
//                requests.add(captureBuilder.build());
//            }

            mCaptureSession.capture(captureBuilder.build(), mTakePictureCallback, mBackgroundHandler);
            mImPreviewReader.close();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void showToast(final String text) {
        final Activity activity = getActivity(mContext);
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void checkFaces(Face[] faces) {
        if (faces != null) {
            Log.d("FACES", String.format("n = %d", faces.length));
        } else {

        }
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */

    private Matrix mFaceDetectionMatrix;
    private OverlayView mOverlayView;

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
       /*     if (mFaceDetectionEnabled) {
                Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                Face[] faces = result.get(CaptureResult.STATISTICS_FACES);

                FaceDetector.detectFace(faces, mode, activeArraySizeRect, mPreviewSize);
            }*/
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    //checkFaces(result.get(CaptureResult.STATISTICS_FACES));

                    // Save exposure and ISO value from Auto-Exposure mode
                    if (result != null) {
                        if (result.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null) {
                            Constants.mAutoExposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                        }
                        if (result.get(CaptureResult.SENSOR_SENSITIVITY) != null) {
                            Constants.mAutoISOValue = result.get(CaptureResult.SENSOR_SENSITIVITY);
                        }

                        // Notify UI
                        mFrameHandler.onExposureChanged(Constants.mAutoExposureTime, Constants.mAutoISOValue);
                    }

                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }
    };


}

