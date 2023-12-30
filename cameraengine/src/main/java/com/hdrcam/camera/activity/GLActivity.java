package com.hdrcam.camera.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hdrcam.camera.ImageProcessingListener;
import com.hdrcam.camera.R;
import com.hdrcam.camera.capture.FrameHandler;
import com.hdrcam.camera.capture.VideoCameraPreview;
import com.hdrcam.camera.render.DispatchingReviewRenderer;
import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ImageData;
import com.hdrcam.camera.utils.ImgUtils;
import com.hdrcam.camera.utils.RawImageData;
import com.hdrcam.camera.utils.RenderScriptHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GLActivity extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback, FrameHandler {
    private static final String TAG = "GLActivity";
    private static final String mPrefix = "VAI_";
    public static Boolean mActivityIsRunning;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_WRITE_PERMISSION = 2;
    private static final int REQUEST_READ_PERMISSION = 3;

    private static final String FRAGMENT_DIALOG = "dialog";
    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA
    };

    protected VideoCameraPreview mMainPreview = null;
    protected VideoCameraPreview mSubPreview = null;
    protected DispatchingReviewRenderer renderer = null;
    private VideoCameraPreview mNormalRearMainPreview = null;
    private VideoCameraPreview mFrontMainPreview = null;
    private VideoCameraPreview mWideRearMainPreview = null;
    private ErrorDialog mErrorDialog;
    private boolean useRear, useWide;

    private ByteBuffer byteBuffer = null;

    private OrientationEventListener sensorOrientlistener;
    private int mPhoneRotation;
    private GLSurfaceView mGlSurfaceView;
    private ImageProcessingListener mImageProcessingListener;
    private int mUserPreviewFormat;
    private int mUserSaveFormat;
    private int mDir = 1;
 //   private RenderScript mRs;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Constants.saveIsLocked = false;
        RenderScriptHelper.mRs = RenderScript.create(this);
//        ImgUtils.setRenderScript(mRs, this);
        sensorOrientlistener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                mPhoneRotation = (((orientation + 45) / 90) % 4) * (90);
            }
        };
        if (sensorOrientlistener.canDetectOrientation()) sensorOrientlistener.enable();
        else sensorOrientlistener = null;

        mActivityIsRunning = true;
    }

    protected void setImageProcessingListener(ImageProcessingListener imageProcessingListener) {
        this.mImageProcessingListener = imageProcessingListener;
    }

    protected void setSaveButton(Button saveButt) {
        // saveButt = this.findViewById(R.id.doSave);
        saveButt.setOnClickListener((View v) -> {
            saveButt.setEnabled(false);
            takePicture();
            saveButt.setEnabled(true);
        });
    }

    protected void setUserDefinedSaveSize(Size saveSize) {
        Constants.userDefinedSaveSize = saveSize;
    }

    protected void setUserDefinedPreviewSize(Size previewSize) {
        Constants.userDefinedPreviewSize = previewSize;
    }

    protected void switchFrontRearCamera(boolean useRear) {
        this.useRear = useRear;
        renderer.switchFrontRearRenderer(useRear);
        if (!useRear) {
            mMainPreview = mFrontMainPreview;
            mDir = -1;
        } else {
            mMainPreview = useWide ? mWideRearMainPreview : mNormalRearMainPreview;
            mDir = 1;
        }
    }

    protected void switchNormalWideCamera(boolean useWide) {
        if (useRear) {
            this.useWide = useWide;
            renderer.switchNormalWideRenderer(useWide);
            mMainPreview = useWide ? mWideRearMainPreview : mNormalRearMainPreview;
        }
    }
    protected void setSaverWithoutCallInsertMedia(boolean saverWithoutCallInsertMedia) {
        Constants.saveWithoutCallInsertMedia = saverWithoutCallInsertMedia;
    }

    protected void setRenderer(String mode, GLSurfaceView glSurfaceView,
                               int userPreviewFormat,
                               int userSaveFormat, boolean useFrontCamInSingleMode,
                               boolean autoWhiteBalanceOn) {
        setRenderer(mode, glSurfaceView, userPreviewFormat,  userSaveFormat,  useFrontCamInSingleMode);
        Constants.autoWhiteBalanceOn = autoWhiteBalanceOn;
    }

    protected void setRenderer(String mode, GLSurfaceView glSurfaceView,
                               int userPreviewFormat,
                               int userSaveFormat, boolean useFrontCamInSingleMode) {
        mGlSurfaceView = glSurfaceView;
        mGlSurfaceView.setEGLContextClientVersion(2);
        if (mode != null) {
            Constants.camMode = mode.equals("dual") ? Constants.CAM_MODE.DUAL :
                    Constants.CAM_MODE.SINGLE;
            mUserPreviewFormat = userPreviewFormat;
            mUserSaveFormat = userSaveFormat;

            if (Constants.camMode == Constants.CAM_MODE.SINGLE) {
                Log.d(TAG, "mode single_cam");
                initSingle(userPreviewFormat, userSaveFormat, Constants.FRONT_CAM);
                initSingle(userPreviewFormat, userSaveFormat, Constants.DUAL_MAIN_CAM);
                initSingle(userPreviewFormat, userSaveFormat, Constants.DUAL_SUB_CAM);
                mMainPreview = useFrontCamInSingleMode ? mFrontMainPreview : mNormalRearMainPreview;
                this.useRear = !useFrontCamInSingleMode;
                this.useWide = false;
            } else if (Constants.camMode == Constants.CAM_MODE.DUAL) {
                Log.d(TAG, "mode dual_cam");
                initDual(userPreviewFormat, userSaveFormat);
            }
        } else Constants.camMode = Constants.CAM_MODE.DUAL;
        renderer = new DispatchingReviewRenderer(mNormalRearMainPreview, mSubPreview,
                mWideRearMainPreview, mSubPreview,
                mFrontMainPreview, mSubPreview,
                userPreviewFormat, !useFrontCamInSingleMode);
        // Set the Renderer for drawing on the GLSurfaceView
        mGlSurfaceView.setRenderer(renderer);
        // Render the view only when there is a change in the drawing data
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    // protected void setRenderer(String mode, GLSurfaceView glSurfaceView,
    //                            int userPreviewFormat,
    //                            int userSaveFormat, boolean useFrontCamInSingleMode, int width,
    //                            int height) {
    //     mGlSurfaceView = glSurfaceView;
    //     mGlSurfaceView.setEGLContextClientVersion(2);
    //     if (mode!=null) {
    //         Constants.camMode = mode.equals("dual") ? Constants.CAM_MODE.DUAL :
    //                 Constants.CAM_MODE.SINGLE;
    //         mUserPreviewFormat = userPreviewFormat;
    //         mUserSaveFormat = userSaveFormat;
    //         if (useFrontCamInSingleMode) {
    //             Constants.cameraIDForSingleMode = Constants.FRONT_CAM;
    //             mDir = -1;
    //         } else Constants.cameraIDForSingleMode = Constants.DUAL_MAIN_CAM;
    //         if (Constants.camMode == Constants.CAM_MODE.SINGLE) {
    //             Log.d(TAG, "mode single_cam");
    //             initSingle(userPreviewFormat, userSaveFormat);
    //         } else if (Constants.camMode == Constants.CAM_MODE.DUAL) {
    //             Log.d(TAG, "mode dual_cam");
    //             initDual(userPreviewFormat, userSaveFormat);
    //         }
    //     } else Constants.camMode = Constants.CAM_MODE.DUAL;
    //     renderer = new PreviewRenderer(width, height, userPreviewFormat);
    //     // Set the Renderer for drawing on the GLSurfaceView
    //     mGlSurfaceView.setRenderer(renderer);
    //     // Render the view only when there is a change in the drawing data
    //     mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    // }

    private void initSingle(int previewFormat, int saveFormat, String cameraID) {
        switch (cameraID) {
            case Constants.FRONT_CAM:
                mFrontMainPreview = new VideoCameraPreview(this, Constants.CAM_MODE.SINGLE,
                        Constants.FRONT_CAM, previewFormat, saveFormat, true);
                break;
            case Constants.DUAL_MAIN_CAM:
                mNormalRearMainPreview = new VideoCameraPreview(this, Constants.CAM_MODE.SINGLE,
                        Constants.DUAL_MAIN_CAM, previewFormat, saveFormat, true);
                break;
            case Constants.DUAL_SUB_CAM:
                mWideRearMainPreview = new VideoCameraPreview(this, Constants.CAM_MODE.SINGLE,
                        Constants.DUAL_SUB_CAM, previewFormat, saveFormat, true);
                break;
        }
        mSubPreview = null;
    }

    private void initDual(int previewFormat, int saveFormat) {
        mMainPreview = new VideoCameraPreview(this, Constants.CAM_MODE.DUAL,
                Constants.DUAL_MAIN_CAM,
                previewFormat, saveFormat, false);

        mSubPreview = new VideoCameraPreview(this, Constants.CAM_MODE.DUAL, Constants.DUAL_SUB_CAM,
                previewFormat, saveFormat, false);
    }

    protected void takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestWritePermission();
            return;
        }
        if (Constants.saveIsLocked) return;

        Log.d(TAG, "----- new capture session -----");
        Constants.saveIsLocked = true;
        Constants.mainSavedBMP = null;
        Constants.subSavedBMP = null;
        Constants.mainSavedImData = null;
        Constants.subSavedImData = null;
        Constants.rawSavedImData = new RawImageData(null, null, null);

        boolean prepare = mMainPreview.prepareTakePicture(mPhoneRotation);
        if (mSubPreview != null) prepare = mSubPreview.prepareTakePicture(mPhoneRotation);

        if (!prepare) return;
        mMainPreview.takePicture(mPhoneRotation);
        if (mSubPreview != null) {
            mSubPreview.takePicture(mPhoneRotation);
        }
    }

    protected void switchCamera() {
        if (Constants.camMode != Constants.CAM_MODE.SINGLE) {
            Log.d(TAG, "Switch camera only support single mode");
            return;
        }
        this.onPause();
        if (mMainPreview.getCameraId().equals(Constants.FRONT_CAM)) mMainPreview.setCameraId(Constants.DUAL_MAIN_CAM);
        else mMainPreview.setCameraId(Constants.FRONT_CAM);
        this.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorOrientlistener != null) sensorOrientlistener.disable();

    }


    @Override
    public void onResume() {
        super.onResume();
        Constants.skipFrames = 0;
        Constants.totalFrames = 0;
        if (mGlSurfaceView != null) mGlSurfaceView.onResume();
        mActivityIsRunning = true;


        if (!hasCameraPermissionsGranted(CAMERA_PERMISSIONS)) {
            requestCameraPermission();
        } else {
            if (mMainPreview != null) mMainPreview.startCamera();
            if (mSubPreview != null) mSubPreview.startCamera();
        }


    }

    @Override
    public void onPause() {
        mActivityIsRunning = false;

        if (hasCameraPermissionsGranted(CAMERA_PERMISSIONS)) {
            if (mMainPreview != null) mMainPreview.stopCamera();
            if (mSubPreview != null) mSubPreview.stopCamera();
        }

        if (mGlSurfaceView != null) mGlSurfaceView.onPause();
        super.onPause();
    }


    private void glRender(byte[] rgbaData, int width, int height) {

        ByteBuffer mPixelBuf = ByteBuffer.allocateDirect(width * height * 4);
        mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);

        mPixelBuf.put(rgbaData);
    }

    private void glSetAndDisplay(ImageData imageData) {
        if (renderer.getmImageData() != null || imageData == null) return;
        renderer.setPreviewData(imageData);
        mGlSurfaceView.requestRender();
    }


    @Override
    public void onSingleFramePreview(ImageData mainData) {
        Log.d(TAG, "start on single frame bef");
        if (mainData == null) return;
        Log.d(TAG, "start on single frame");


        int orientation = 0;
        if (mMainPreview !=null)
            orientation = (mMainPreview.getSensorOrientation() + mDir * mPhoneRotation) % 360;
        mainData.setOrientation(orientation);
        //Log.d(TAG, "orient" + Constants.mainPreviewImData.getOrientation());
        if (mImageProcessingListener != null) {
            ImageData processedData =
                    mImageProcessingListener.doSingleImageProcessingForPreview(mainData);
            glSetAndDisplay(processedData);
        } else {
            glSetAndDisplay(mainData);
        }
    }

    @Override
    public void onDualFramePreview(ImageData mainData, ImageData subData) {
        if (mainData == null || subData == null) return;
        Log.d(TAG, "on dual frame processing");
        int orientation = 90;
        if (mMainPreview!=null)
            orientation = (mMainPreview.getSensorOrientation() + mDir * mPhoneRotation) % 360;
        mainData.setOrientation(orientation);
        subData.setOrientation(orientation);

        if (mImageProcessingListener != null) {
            ImageData processedImage =
                    mImageProcessingListener.doDualImageProcessingForPreview(mainData, subData);
            glSetAndDisplay(processedImage);
        } else {
            glSetAndDisplay(mainData);
        }
    }

    @Override
    public void doSave(Context context, ImageData mainData, ImageData subData) throws Exception {
        Log.d(TAG, "doSave() calling");
        boolean result = false;
        if (Constants.camMode == Constants.CAM_MODE.SINGLE) {
            Log.d(TAG, "Single save");
            result = doSaveSingleImage(context, mainData);
        } else if (Constants.camMode == Constants.CAM_MODE.DUAL) {
            //Log.d(TAG, "dual save - waiting save");
      /*      Thread thread = new Thread() {
                public void run() {
                    int maxWaitingTime = 10000;
                    int waitingTime = 20;
                    int totalWaitingTime = 0;
                    while (Constants.subSavedImData == null && totalWaitingTime < maxWaitingTime) {
                        try {
                            Thread.sleep(waitingTime);
                            totalWaitingTime += waitingTime;
                        } catch (InterruptedException e) {
                            Log.d(TAG, "do save exception while waiting other cam");
                            e.printStackTrace();
                        }
                    }
                }
            };
            thread.start();
            thread.join();*/

            //Log.d(TAG, "start save dual - ending with " + Constants.subSavedImData);
            //if (Constants.subSavedImData == null) throw new NullPointerException();
            Log.d(TAG, "start save dual - do save");
            //if (Constants.subSavedImData != null && Constants.subSavedImData.getData() != null) {
            synchronized (Constants.saveLock) {
                Log.d(TAG, "start save dual - do save - in lock");
                result = doSaveDualImage(context, mainData, subData);
            }
            //  }
        }

        if (result) postSave();
        Log.d(TAG, "========post save========");
    }

    @Override
    public void doSave(Image rawImage, CameraCharacteristics cameraCharacteristics, CaptureResult result, String burstTimestamp) {
        int width = rawImage.getWidth();
        int height = rawImage.getHeight();
        Log.d(TAG, String.format("raw image saving %d %d %d", rawImage.getFormat(),
                rawImage.getWidth(), rawImage.getHeight()));
        ShortBuffer rawBuffer = rawImage.getPlanes()[0].getBuffer().asShortBuffer();
        rawBuffer.rewind();

        DngCreator dngCreator = new DngCreator(cameraCharacteristics, result);
        FileOutputStream output = null;
        File rawFile = new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "RAW_" + burstTimestamp + ".dng");
        try {
            output = new FileOutputStream(rawFile);
            dngCreator.writeImage(output, rawImage);
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, rawFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng"); // or image/png
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[] { rawFile.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            //success = true;
        } catch (Exception e) {
            Log.d("doSaveRAW", "Exception");
            e.printStackTrace();
        } finally {
            //rawImage.close();
            closeOutput(output);
        }
        postSave();

        if (mImageProcessingListener != null)
            mImageProcessingListener.doSingleRAWProcessingForSave(rawBuffer, width, height);
    }

    @Override
    public void doSave(ImageData imageData, String burstTimestamp) {
        FileOutputStream output = null;
        File jpegFile = new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "JPEG_" + burstTimestamp + ".jpg");
        try {
            output = new FileOutputStream(jpegFile);
            output.write(imageData.getData());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, jpegFile.getAbsolutePath());
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // or image/png
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[]{jpegFile.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
            //success = true;
        } catch (Exception e) {
            Log.d("doSaveRAW", "Exception");
            e.printStackTrace();
        } finally {
            //rawImage.close();
            closeOutput(output);
        }
        postSave();
    }

    @Override
    public void onSingleRaw(RawImageData image, int index) {
        Log.d(TAG, "Not implemented");
    }

    @Override
    public void onSingleRaw(ImageData image, int index) {
        Log.d(TAG, "Not implemented");
    }

    @Override
    public void onSingleJPEG(ImageData imageData, int index) {
        Log.d(TAG, "Not implemented");
    }

    @Override
    public void onBurstStart() {
        Log.d(TAG, "Not implemented");
    }

    @Override
    public void onBurstComplete() {
        Log.d(TAG, "Not implemented");
    }

    @Override
    public void autoExposureOnPreview(int val) {Log.d(TAG, "Not implemented");}

    @Override
    public void onExposureChanged(Long exposureTime, Integer iso) {
        Log.d(TAG, "Not implemented");
    }

    /**
     * Cleanup the given {@link OutputStream}.
     *
     * @param outputStream the stream to close.
     */
    private static void closeOutput(OutputStream outputStream) {
        if (null != outputStream) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getBMP(ImageData imageData) {
        switch (imageData.getFormat()) {
            case ImageFormat.JPEG:
                return ImgUtils.convertJPEGBytesToBMP(imageData.getData());
            case ImageFormat.NV21:
                YuvImage YUVImage = new YuvImage(imageData.getData(), ImageFormat.NV21, imageData.getWidth(),
                        imageData.getHeight(), null);

                if (YUVImage != null) {

                    try {

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();

                        YUVImage.compressToJpeg(new Rect(0, 0, imageData.getWidth(),
                                imageData.getHeight()), 100, stream);

                        Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                        stream.close();

                        return bmp;

                    } catch (IOException e) {
                        return null;
                    }

                }
            default:
                return null;
        }
    }

    private static void postSave() {
        Constants.subSavedImData = null;
        Constants.mainSavedImData = null;

        if (Constants.mainSavedBMP != null) Constants.mainSavedBMP.recycle();
        if (Constants.subSavedBMP != null) Constants.subSavedBMP.recycle();

        Constants.mainSavedBMP = null;
        Constants.subSavedBMP = null;

        Constants.saveIsLocked = false;
    }

    private boolean doSaveSingleImage(Context context, ImageData mainData) {
        Log.d(TAG, "doSaveSingleImage()");
        if (mImageProcessingListener == null)
            return false;


        ImageData processedData =
                mImageProcessingListener.doSingleImageProcessingForSave(mainData);
/*        saveBytes(Constants.mainSavedImData.getData(), "NV21FOLDER",
                Constants.mainSavedImData.getWidth(), Constants.mainSavedImData.getHeight());*/

        // Bitmap bokehBMP = getBMP(bokehData);
        if (processedData==null) return false;
        Bitmap processedBMP = ImgUtils.ImageDataToBitmap(processedData);

        try {
//            if (Constants.saveWithoutCallInsertMedia) {
//               // Log.d(TAG, "do_save without insert media");
//               String savedFilePath =  ImgUtils.saveBMPToSDCard(processedBMP, this);
//            } else {
//              //  Log.d(TAG, "do_save insert media");
//
//                String savedFilePath = ImgUtils.insertImage(context.getContentResolver(),
//                        processedBMP, "bokeh", "Bokeh Image",
//                        "bokeh", System.currentTimeMillis(), processedData.getOrientation());
//            }
            String savedFilePath = ImgUtils.saveImageForQ(processedBMP, mPrefix, context);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean doSaveDualImage(Context context, ImageData mainData, ImageData subData) {

        if (mainData== null || subData == null) {
//            Log.d(TAG,
//                    "Main is " + Constants.mainSavedImData + " Sub is " + Constants
//                    .subSavedImData);
            return false;
        }
        if (mImageProcessingListener == null) return false;
        Log.d(TAG, "start save dual - do save - 2 non null");

        ImageData processedData = mImageProcessingListener.doDualImageProcessingForSave(mainData, subData);
        if (processedData==null) return false;

        if (mainData.getData().length == mainData.getHeight() * mainData.getWidth() * 1.5) {
            Log.d(TAG,
                    "size ok " + mainData.getData().length + " " + mainData.getWidth() + " " + mainData.getHeight());
        } else
            Log.d(TAG, "size not ok");
        //saveBytes(Constants.mainSavedImData.getData(), "NV21FOLDER");
        Bitmap processedBMP = ImgUtils.ImageDataToBitmap(processedData);
        //getBMP
        // (processedData);
        Bitmap subBMP = ImgUtils.ImageDataToBitmap(subData);
        //getBMP
        // (Constants
        // .subSavedImData);
        try {
//            if (Constants.saveWithoutCallInsertMedia) {
//             //   Log.d(TAG, "do_save without insert media");
//
//                ImgUtils.saveBMPToSDCard(processedBMP, this);
//                ImgUtils.saveBMPToSDCard(subBMP, this);
//
//            }
//            else {
//             //   Log.d(TAG, "do_save with insert media");
//
//                ImgUtils.insertImage(context.getContentResolver(), processedBMP,
//                        "bokeh_main", "Bokeh Image", "bokeh", System.currentTimeMillis(),
//                        processedData.getOrientation());
//                ImgUtils.insertImage(context.getContentResolver(), subBMP, "bokeh_sub",
//                        "Bokeh Image", "bokeh", System.currentTimeMillis(),
//                        subData.getOrientation());
//            }
            String savedFilePath1 = ImgUtils.saveImageForQ(processedBMP, mPrefix, context);
            String savedFilePath2 = ImgUtils.saveImageForQ(subBMP, mPrefix + "sub_", context);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            Constants.saveIsLocked = false;
        }
    }


    private byte[] doBokeh2d(Bitmap bmp) {
        int size = bmp.getRowBytes() * bmp.getHeight();

        byteBuffer = ByteBuffer.allocate(size);
        bmp.copyPixelsToBuffer(byteBuffer);

        return byteBuffer.array();

    }


    private byte[] doBokeh3d(Bitmap rgba, Bitmap mono) {
        int size = rgba.getRowBytes() * rgba.getHeight();

        byteBuffer = ByteBuffer.allocate(size);
        rgba.copyPixelsToBuffer(byteBuffer);

        return byteBuffer.array();

    }

//    private void saveBytes(byte[] data, String folderName, int w, int h) {
//        try {
//            ImgUtils.saveBytesToFile(data, folderName, w, h);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length == CAMERA_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        if (null == mErrorDialog || mErrorDialog.isHidden()) {
                            mErrorDialog = ErrorDialog.newInstance(getString(R.string.request_permission));
                            mErrorDialog.show(getSupportFragmentManager(), FRAGMENT_DIALOG);
                        }
                        break;
                    } else {
                        if (null != mErrorDialog) {
                            mErrorDialog.dismiss();
                        } else {
                            if (mMainPreview != null) mMainPreview.startCamera();
                            if (mSubPreview != null) mSubPreview.startCamera();
                        }
                    }
                }
            }
        } /*else if (requestCode == REQUEST_WRITE_PERMISSION){
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                mErrorDialog =
                        ErrorDialog.newInstance(getString(R.string.request_write_permission));
                mErrorDialog.show(getSupportFragmentManager(), FRAGMENT_DIALOG);
            }
            else if (null != mErrorDialog) {
                mErrorDialog.dismiss();
            }
        }*/else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasCameraPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void requestWritePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new ConfirmationDialog().show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_PERMISSION);
        }
    }


    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }
    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity, CAMERA_PERMISSIONS,
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    activity.finish();
                                }
                            })
                    .create();
        }
    }


}