package com.hdrcam.camera.utils;

public class ImageData {
    private byte[] mData;
    private int mWidth;
    private int mHeight;

    private int mOrientation;
    private int mImageFormat;

    public ImageData(byte[] mData, int width, int height, int imageFormat, int rotation) {
        this.mData = mData;
        this.mWidth = width;
        this.mHeight = height;
        this.mImageFormat = imageFormat;
        this.mOrientation = rotation;
    }

    public ImageData() {

    }

    public byte[] getData() {
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

    public int getFormat() {
        return mImageFormat;
    }


}
