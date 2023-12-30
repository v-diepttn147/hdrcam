package com.hdrcam.camera.utils;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;

public class ShortImageData {

    public ShortImageData(short[] mData, int width, int height, int rotation, CameraCharacteristics cameraCharacteristics, CaptureResult captureResult) {
        this.mData = mData;
        this.mWidth = width;
        this.mHeight = height;
        this.mOrientation = rotation;
        this.cameraCharacteristics = cameraCharacteristics;
        this.captureResult = captureResult;
    }

    public ShortImageData() {

    }

    public short[] getData() {
        return mData;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return cameraCharacteristics;
    }

    public CaptureResult getCaptureResult() {
        return captureResult;
    }

    private short[] mData;
    private int mWidth;
    private int mHeight;
    private int mOrientation;
    private CameraCharacteristics cameraCharacteristics;
    private CaptureResult captureResult;
}
