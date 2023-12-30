package com.hdrcam.camera.utils;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hdrcam.camera.ScriptC_rgba_to_yuv;
import com.hdrcam.camera.ScriptC_yuv_to_rgba;
import com.hdrcam.camera.ScriptC_rgba2i420;

public class RenderScriptHelper {
    public static RenderScript mRs;
    public static byte[] convertYuvToRgbIntrinsic(byte[] data, int imageWidth,
                                                  int imageHeight) {
        //  long start = System.currentTimeMillis();
        if (checkIfRsInitSuccess()) return null;

        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(mRs, Element.RGBA_8888(mRs));

        // Create the input allocation  memory for Renderscript to work with
        Type.Builder yuvType = new Type.Builder(mRs, Element.U8(mRs))
                .setX(imageWidth)
                .setY(imageHeight)
                .setYuvFormat(ImageFormat.NV21);

        Allocation aIn = Allocation.createTyped(mRs, yuvType.create(), Allocation.USAGE_SCRIPT);
        // Set the YUV frame data into the input allocation
        aIn.copyFrom(data);


        // Create the output allocation
        Type.Builder rgbType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
                .setX(imageWidth)
                .setY(imageHeight);

        Allocation aOut = Allocation.createTyped(mRs, rgbType.create(), Allocation.USAGE_SCRIPT);


        yuvToRgbIntrinsic.setInput(aIn);

        // Run the script for every pixel on the input allocation and put the result in aOut
        yuvToRgbIntrinsic.forEach(aOut);

        byte[] outBytes = new byte[imageWidth * imageHeight * 4];
        aOut.copyTo(outBytes);

        return outBytes;

    }


    public static byte[] convert_bitmap_to_yuv(@NonNull Bitmap bmp, int format) {
        if (checkIfRsInitSuccess()) return null;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int yuv_len = width * height * 3/2;

        Allocation aIn_ = Allocation.createFromBitmap(mRs, bmp);


        Type.Builder typeYuv = new Type.Builder(mRs, Element.U8(mRs));
        typeYuv.setX(yuv_len);
        Allocation yuv_out = Allocation.createTyped(mRs, typeYuv.create());

        ScriptC_rgba_to_yuv scriptC_rgba_to_yuv= new ScriptC_rgba_to_yuv(mRs);

        scriptC_rgba_to_yuv.set_width(width);
        scriptC_rgba_to_yuv.set_y_len(width * height);
        scriptC_rgba_to_yuv.set_yuv_out(yuv_out);

        if (format == ExImageFormat.NV21) {
            scriptC_rgba_to_yuv.forEach_rgba_to_nv21_convert_full_swing(aIn_);
        } else if (format == ExImageFormat.NV12) {
            scriptC_rgba_to_yuv.forEach_rgba_to_nv12_convert_full_swing(aIn_);
        } else {
            return null;
        }

        byte[] outBytes = new byte[yuv_len];
        yuv_out.copyTo(outBytes);

        return outBytes;

    }

    public static byte[] convert_rgba_to_i420(RenderScript rs, Bitmap bmp) {
        if (checkIfRsInitSuccess()) return null;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int y_len = width * height;
        int uv_len = width*height/4;

        ScriptC_rgba2i420 scriptC_rgba2i420 = new ScriptC_rgba2i420(rs);

        Allocation aIn_ = Allocation.createFromBitmap(rs, bmp);


        Type.Builder  yuvType_ = new Type.Builder(rs, Element.U8(rs))
                    .setX(width)
                    .setY(height);
        Allocation yuv_out_ = Allocation.createTyped(rs, yuvType_.create(),
                    Allocation.USAGE_SCRIPT);
            Type.Builder typeUcharUV = new Type.Builder(rs, Element.U8(rs));
            typeUcharUV.setX(uv_len);
        Allocation u_out = Allocation.createTyped(rs, typeUcharUV.create());
        Allocation v_out = Allocation.createTyped(rs, typeUcharUV.create());


        scriptC_rgba2i420.set_uvRowStride(width/2);
        scriptC_rgba2i420.set_u_out(u_out);
        scriptC_rgba2i420.set_v_out(v_out);
        // scriptC_rgba_to_i420.set_width(width);
        scriptC_rgba2i420.forEach_convert(aIn_, yuv_out_);

        byte[] y_bytes = new byte[y_len];
        byte[] u_bytes = new byte[uv_len];
        byte[] v_bytes = new byte[uv_len];

        yuv_out_.copyTo(y_bytes);
        u_out.copyTo(u_bytes);
        v_out.copyTo(v_bytes);

        byte[] yuv_merge = new byte[y_len*3/2];
        System.arraycopy(y_bytes, 0, yuv_merge, 0, y_bytes.length);
        System.arraycopy(u_bytes, 0, yuv_merge, y_bytes.length, u_bytes.length);
        System.arraycopy(v_bytes, 0, yuv_merge, y_bytes.length*5/4, v_bytes.length);

        //YuvData yuvData = new YuvData(YuvData.I420, y_bytes, u_bytes, v_bytes, width, height);
        return yuv_merge;

    }
    private static boolean checkIfRsInitSuccess()  {
        if (mRs == null) {
            Log.e("RenderScriptHelper", "Render Script is Null. Quitting now.");
            return true;
        }

        Log.i("RenderScriptHelper", "Render Script is ready. Converting now.");

        return false;
    }
    public static Bitmap convert_YUV_To_Bitmap(byte[] data, int imageWidth, int imageHeight,
                                               int format) {
       if (checkIfRsInitSuccess()) return null;

        // Input allocation
        Type.Builder yuvType = new Type.Builder(mRs, Element.createPixel(mRs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV))
                .setX(imageWidth)
                .setY(imageHeight)
                .setMipmaps(false)
                .setYuvFormat(ImageFormat.NV21);
        Allocation ain = Allocation.createTyped(mRs, yuvType.create(), Allocation.USAGE_SCRIPT);
        ain.copyFrom(data);


        // output allocation
        Type.Builder rgbType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
                .setX(imageWidth)
                .setY(imageHeight)
                .setMipmaps(false);

        Allocation aOut = Allocation.createTyped(mRs, rgbType.create(), Allocation.USAGE_SCRIPT);


        // Create the script
        ScriptC_yuv_to_rgba yuvScript = new ScriptC_yuv_to_rgba(mRs);
        // Bind to script level -  set the allocation input and parameters from the java into the script level (thru JNI)
        yuvScript.set_ypsIn(ain);
        yuvScript.set_y_len(imageWidth*imageHeight);
        yuvScript.set_u_len(imageWidth*imageHeight/4);
        yuvScript.set_uvRowStride(imageWidth);

        // invoke the script conversion method
        if (format == ExImageFormat.NV12) {
            yuvScript.forEach_nv12_doConvert_opencv_original(aOut);
        } else if (format==ExImageFormat.NV21) {
            yuvScript.forEach_nv21_doConvert_float_1(aOut);

        }
        Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        aOut.copyTo(outBitmap) ;

        return outBitmap ;

    }
}
