package com.hdrcam.camera.capture;

import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ImgUtils;

import java.nio.ByteBuffer;

/**
 * Created by oleg on 11/2/17.
 */

public class PreviewImageListener implements ImageReader.OnImageAvailableListener {

    private FrameHandler mPreviewFrameHandler;
    private Constants.CAM_MODE mMode;
    private String mCameraId;
    private int mPreviewFormat;
   // private Image mMonoImage = null;

    public PreviewImageListener(FrameHandler frameHandler, Constants.CAM_MODE mode,
                                String cameraId, int previewFormat) {

        mPreviewFrameHandler = frameHandler;
        mMode = mode;
        mCameraId = cameraId;
        mPreviewFormat = previewFormat;
    }

    private void callSyncPreview() {
        synchronized (Constants.previewLock) {
            if (mMode == Constants.CAM_MODE.SINGLE) {
                mPreviewFrameHandler.onSingleFramePreview(Constants.mainPreviewImData);
            }
            else {
                mPreviewFrameHandler.onDualFramePreview(Constants.mainPreviewImData, Constants.subPreviewImData);
            }

        }
    }

    private void calculateBrightnessLevel() {
        long start = System.currentTimeMillis();
        byte[] nv = Constants.mainPreviewImData.getData();
        int yByteLen = Constants.mainPreviewImData.getWidth() * Constants.mainPreviewImData.getHeight();
        int stride = 5, width = Constants.mainPreviewImData.getWidth(), height = Constants.mainPreviewImData.getHeight();
        int avgLuminance = 0, count = width / stride * height / stride;
//        for (int i = 0; i < yByteLen; i += stride) {
//            avgLuminance += nv[i] & 0xff;
//        }
        for (int i = 0; i < height; i += stride){
            for (int j = 0; j < width; j += stride) {
                avgLuminance += nv[i * width + j] & 0xff;
            }
        }
        avgLuminance /= count;
        Log.d("autoExposureOnPreview", "Time: " + (System.currentTimeMillis() - start) + " ms");
        mPreviewFrameHandler.autoExposureOnPreview(avgLuminance);
    }

    @Override
    public void onImageAvailable(ImageReader imageReader) {
        if (!mCameraId.equals(Constants.DUAL_SUB_CAM)) {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Constants.totalFrames++;
                if (Constants.mainPreviewImData == null) {
                    Constants.mainPreviewImData = ImgUtils.convertToImageData(image, mPreviewFormat);
                    Log.d("CAMERAPREVIEW", "new rgb");
                    image.close();
                    callSyncPreview();
//                    calculateBrightnessLevel();
                }
                else {
                    image.close();
                    Log.d("CAMERAPREVIEW",
                            "RGB skip " + ++Constants.skipFrames + " frames in " + Constants.totalFrames);
                }


            }
        } else {
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                if (Constants.subPreviewImData == null) {
                    Constants.subPreviewImData = ImgUtils.convertToImageData(image,mPreviewFormat);
                    Log.d("CAMERAPREVIEW", "new mono");
                    image.close();
                    callSyncPreview();
                }
                else
                    image.close();
            }

        }
    }

}
