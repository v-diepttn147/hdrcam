package com.hdrcam.camera.capture;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ImageData;
import com.hdrcam.camera.utils.ImgUtils;
import com.hdrcam.camera.utils.RawImageData;

import java.nio.ByteBuffer;

public class SavedImageListener implements ImageReader.OnImageAvailableListener {
    public final FrameHandler mSaveHandler;
    private Handler mBackgroundHandler;
    private int mOrientation;
    private String mCameraId;
    private Context mContext;
    private Constants.CAM_MODE mMode;
    private int mUserSaveFormat;

    private CameraCharacteristics mCameraCharacteristics;
    private CaptureResult mCaptureResult;
    private int burstSize;
    private int burstIndex = 0;

    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
    }

    public void setCaptureResult(CaptureResult result) {
        this.mCaptureResult = result;
    }

    public void setBurstSize(int burstSize) {
        this.burstSize = burstSize;
    }

    public void setRotation(int rotation) {
        this.mOrientation = rotation;
    }

    public void setBackgroundHandler(Handler mBackgroundHandler) {
        this.mBackgroundHandler = mBackgroundHandler;
    }

    public SavedImageListener(FrameHandler frameHandler, Context context,
                              Constants.CAM_MODE mode, String cameraId, int saveFormat) {

        mMode = mode;
        mCameraId = cameraId;
        mContext = context;
        mSaveHandler = frameHandler;
        mUserSaveFormat = saveFormat;
    }

    private void onSingleRaw(Image image) {
        if (burstIndex == 0) {
            mSaveHandler.onBurstStart();
        }
        // RawImageData rawImageData = new RawImageData(image, mCameraCharacteristics, mCaptureResult);
        Constants.rawSavedImData = new RawImageData(image, mCameraCharacteristics, mCaptureResult);
        burstIndex++;
        if (burstIndex >= burstSize) {
            mSaveHandler.onBurstComplete();
            burstIndex = 0;
        }
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        ImageData imageData = new ImageData(bytes, image.getWidth(), image.getHeight(), image.getFormat(), mOrientation);
        image.close();
        mSaveHandler.onSingleRaw(imageData, burstIndex);
    }

    private void onSingleJPEG(ImageData imageData) {
        if (burstIndex == 0) {
            mSaveHandler.onBurstStart();
        }

        burstIndex++;
        if (burstIndex >= burstSize) {
            mSaveHandler.onBurstComplete();
            burstIndex = 0;
        }
        mSaveHandler.onSingleJPEG(imageData, burstIndex);
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
     /*   if (mUserSaveFormat == ImageFormat.RAW_SENSOR) {
            synchronized (Constants.saveLock) {
                Log.d("rawsave", "on image available");
                Image image = imageReader.acquireLatestImage();
                if (image!=null) {
                    if (Constants.rawSavedImData.getRawImage() == null) {

                        Log.d("rawsave", "set main saved data from on available");
                        Constants.rawSavedImData.setRawImage(image);
                        Constants.rawSavedImData.setCameraCharacteristics(mCameraCharacteristics);
                        if (Constants.rawSavedImData.getCaptureResult()!=null) {
                            Log.d("rawsave", "dosave from on available");

                            mSaveHandler.doSave(Constants.rawSavedImData.getRawImage(),
                                    Constants.rawSavedImData.getCameraCharacteristics(),
                                    Constants.rawSavedImData.getCaptureResult());
                            //image.close();
                        }
                    }
                }
            }
        }*/

        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                Image image = imageReader.acquireNextImage();
                if (image != null) {
                    Log.d("xxx",
                            mCameraId + " start save WH " + image.getWidth() + " " + image.getHeight() + " " + image.getFormat());

                    int imageFormat = image.getFormat();

                    if (imageFormat == ImageFormat.RAW_SENSOR) {
                        onSingleRaw(image);

                    } else if (imageFormat == ImageFormat.JPEG) {
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        ImageData imageData = new ImageData(bytes, image.getWidth(), image.getHeight(), imageFormat, mOrientation);
                        image.close();

                        onSingleJPEG(imageData);
                    } else {
                        ImageData imageData = ImgUtils.convertToImageData(image, mUserSaveFormat);
                        image.close();
                        try {
                            imageData.setOrientation(mOrientation);

                            if (mCameraId.equals(Constants.DUAL_MAIN_CAM) || mMode ==
                                    Constants.CAM_MODE.SINGLE) {
                                Log.d("xxx", "start save from main camera");

                                if (Constants.mainSavedImData == null) {
                                    synchronized (Constants.saveLock) {
                                        Constants.mainSavedImData = imageData;
                                    }
                                    if (Constants.mainSavedImData != null)
                                        mSaveHandler.doSave(mContext, Constants.mainSavedImData, Constants.subSavedImData);
                                }
                                //SaveImageHandlerBak.doSave(mContext,mRotation);
                                showToast("Saved successfully to sdcard", Gravity.TOP);
                            } else {
                                // Constants.subSavedImData = imageData;

                                if (Constants.subSavedImData == null) {
                                    synchronized (Constants.saveLock) {
                                        Constants.subSavedImData = imageData;
                                    }
                                    if (Constants.subSavedImData != null) {
                                        mSaveHandler.doSave(mContext, Constants.mainSavedImData, Constants.subSavedImData);
                                    }
                                }
                                Log.d("xxx", "start save from sub camera" + Constants.subSavedImData);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("onSave", "Save failed.");
                            showToast("Save failed!", Gravity.TOP);
                        } finally {
                            Log.d("onSave", "start save from main camera finally");
                            // SaveImageHandler.postSave();
                        }
                    }
                }
            }
        });
    }

    private void showToast(final String text, int gravity) {
        final Activity activity = getActivity(mContext);
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
                    toast.setGravity(gravity, 0, 0);
                    toast.show();

                }
            });
        }
    }

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
}
