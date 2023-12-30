package com.hdrcam.camera.utils;

public class NativeConversion {
    public static class YUV_TYPE {

        public static final int NV12 = 10;
        public static final int NV21 = 11;
    }


    public static class RGB_TYPE {

        public static final int RGB = 20;
        public static final int RGBA = 21;
        public static final int BGR = 22;
        public static final int BGRA = 23;
    }
    public static class CONVERSION_TYPE {
        public static final int YUV_TO_RGB_INT_WIKI = 10;
        public static final int YUV_TO_RGB_FLOAT_OPEN_CV_LIKE = 20; //https://docs.opencv.org/4.2.0/de/d25/imgproc_color_conversions.html
        public static final  int YUV_TO_RGB_FLOAT_ANDROID = 21;

        public static final int RGB_TO_YUV_INT_STUDIO_SWING = 30; //https://en.wikipedia.org/wiki/YUV
        public static final int RGB_TO_YUV_INT_FULL_SWING = 31; //https://en.wikipedia.org/wiki/YUV
        public static final int RGB_TO_YUV_FLOAT_OPEN_CV_LIKE = 40;
    }

    /**
     * Convert from YUV to RGB. All YUV channels are stored in single array.
     * @param out_RgbType RGB or BGR output image. See RGB_Type class for the values.
     * @param in_YuvData
     * @param in_YuvType NV12 or NV21 input image. See YUV_Type class for the values.
     * @param width
     * @param height
     * @return the byte array of YUV image
     */

    public static native byte[] convertYuvToRgb(int out_RgbType, byte[] in_YuvData,
                                                int in_YuvType, int width,
                                                int height);


    /**
     * Convert from RGB to YUV. .
     *
     * @param out_YuvType NV12 or NV21 output image. See YUV_Type class for the values.
     * @param in_RgbData
     * @param in_RgbType  RGB or BGR input image. See RGB_Type class for the values.
     * @param width
     * @param height
     * @return the byte array of RGB or BGR image
     */
    public static native byte[] convertRgbToYuv(int out_YuvType, byte[] in_RgbData,
                                                int in_RgbType, int width,
                                                int height);

    /**
     *
     * @param data
     * @param width
     * @param height
     * @return
     */


    public static native byte[] doTestConversion(byte[] data, int width, int height);

    public static native float getLibVersion();
    static {
        System.loadLibrary("img-conversion");
    }


}

/**
 //     * Convert from YUV to RGB. Y and UV channels are stored in 2 arrays.
 //     * @param out_RgbType RGB or BGR output image. See RGB_Type class for the values.
 //     * @param in_yData
 //     * @param in_UvData
 //     * @param in_YuvType NV12 or NV21 input image. See YUV_Type class for the values.
 //     * @param width
 //     * @param height
 //     * @return the byte array of YUV image
 //     */
