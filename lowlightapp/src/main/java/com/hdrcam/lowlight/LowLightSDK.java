package com.hdrcam.lowlight;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

public class LowLightSDK {
    public native void init(AssetManager mgr);


    public native byte[] enhance(byte[] input, int width, int height);

    private int elapsedMs = 0;

    public int getElapsedMs() {
        return elapsedMs;
    }

    static {
        System.loadLibrary("lowlight");
    }
}
