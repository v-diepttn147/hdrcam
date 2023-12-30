package com.hdrcam.camera.utils;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.media.Image;

public class RawImageData {

    public RawImageData(Image rawImage, CameraCharacteristics cameraCharacteristics, CaptureResult result) {
        this.mRawImage = rawImage;
        this.mCameraCharacteristics = cameraCharacteristics;
        this.mCaptureResult = result;
    }

    public Image getRawImage() {
        return mRawImage;
    }
    public void setRawImage(Image mRawImage) {
        this.mRawImage = mRawImage;
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return mCameraCharacteristics;
    }
    public void setCameraCharacteristics(CameraCharacteristics mCameraCharacteristics) {
        this.mCameraCharacteristics = mCameraCharacteristics;
    }

    public CaptureResult getCaptureResult() {
        return mCaptureResult;
    }
    public void setCaptureResult(CaptureResult mCaptureResult) {
        this.mCaptureResult = mCaptureResult;
    }


    private Image mRawImage;
    private CameraCharacteristics mCameraCharacteristics;
    private CaptureResult mCaptureResult;

}