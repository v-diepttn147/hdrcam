package com.hdrcam.camera.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.ExifInterface;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.RenderScript;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;

public class ImgUtils {
    private final static String TAG = "ImgUtils";
    private static RenderScript mRs;
    private static Context mContext;

    public static void setRenderScript(RenderScript renderScript, Context context) {
        mRs = renderScript;
        mContext = context;
    }

    public static Bitmap decodeImageJPEGToBMP(Image image) {

        if (image == null) return null;
        if (image.getFormat() != ImageFormat.JPEG) return null;

        long start1 = System.currentTimeMillis();

        ByteBuffer jpgBuffer = image.getPlanes()[0].getBuffer();
        byte[] byteArray = new byte[jpgBuffer.remaining()];
        jpgBuffer.get(byteArray);
        Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        long finish1 = System.currentTimeMillis();
        long timeElapsed1 = finish1 - start1;
        Log.i("render", String.format("converttobmp() *%d", timeElapsed1));
        return bmp;

    }

    public static Bitmap convertJPEGBytesToBMP(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public static Bitmap rgbaBytesToBitmap(byte[] bitmapdata, int imageWidth, int imageHeight) {
        //Create bitmap with width, height, and 4 bytes color (RGBA)
        Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(bitmapdata);
        bmp.copyPixelsFromBuffer(buffer);
        return bmp;
    }

    public static Bitmap ImageDataToBitmap(ImageData imageData) {

        switch (imageData.getFormat()) {
            case ExImageFormat.CONVERTED_NV21:
            case ExImageFormat.NV21:
                return convert_NV21ToBMP(imageData.getData(),
                        imageData.getWidth(), imageData.getHeight());
            case ExImageFormat.RGBA:
                return ImgUtils.rgbaBytesToBitmap(imageData.getData(),
                        imageData.getWidth(), imageData.getHeight());
            default:
                return null;
        }

    }

    public static byte[] convert_RGB_To_RGBA(byte[] rgb, int width, int height) {
        byte[] rgba = new byte[width*height*4];
        int rgbPix;
        int rgbaPix ;
        for (int i = 0; i< rgb.length/3; i++) {
            rgbPix = i * 3;
            rgbaPix = i * 4;
            rgba[rgbaPix++] = rgb[rgbPix++];
            rgba[rgbaPix++] = rgb[rgbPix++];
            rgba[rgbaPix++] = rgb[rgbPix];
            rgba[rgbaPix] = -1;
        }

        return  rgba;
    }

    public static Bitmap convert_NV21ToBMP(byte[] nv21, int width, int height) {

        byte[] rgb = NativeConversion.convertYuvToRgb(NativeConversion.RGB_TYPE.RGB, nv21,
                NativeConversion.YUV_TYPE.NV21, width, height);
        byte[] rgba = convert_RGB_To_RGBA(rgb, width, height);

        return ImgUtils.rgbaBytesToBitmap(rgba, width, height);
    }

    public static Bitmap convert_decodeImageYUVToBMP(Image image) {
        if (image == null) return null;
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane Y = image.getPlanes()[0];
        Image.Plane UV = image.getPlanes()[1];

        int Yb = Y.getBuffer().remaining();
        int Ub = UV.getBuffer().remaining();

        byte[] data = new byte[Yb + Ub];

        Y.getBuffer().get(data, 0, Yb);
        UV.getBuffer().get(data, Yb, Ub);


        YuvImage YUVImage = new YuvImage(data, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        if (YUVImage != null) {

            try {

                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                YUVImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);

                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                stream.close();

                return bmp;

            } catch (IOException e) {

            }

        }

        return null;
    }

/*    public static byte[] convertYUV420ToNV21(Image image) {
        if (image == null) return null;
        if (image.getFormat() != ImageFormat.YUV_420_888) return null;
        byte[] nv21;
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }*/

    public static Bitmap convert_YUV420ToBMP(Image image) {
        if (image == null) return null;
        if (image.getFormat() != ImageFormat.YUV_420_888) return null;
        byte[] nv21;
        int width = image.getWidth();
        int height = image.getHeight();

        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        //image.close();e

        YuvImage YUVImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        Bitmap bmp = null;

        if (YUVImage != null) {

            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                YUVImage.compressToJpeg(new Rect(0, 0, width, height), 100, stream);

                bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

                stream.close();
            } catch (Exception e) {

                e.printStackTrace();
                return null;
            }
        }
        return bmp;
    }

    public static String saveBMPToSDCard(Bitmap bmp, Context context) throws Exception {
        Log.d(TAG, "do_save without insert media");
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");


        File folder =
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/bokeh");
        String formattedDate =
                folder.getAbsolutePath() +
                        "/" + "bo_" + df.format(c.getTime()) + ".jpg";
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success && folder.isDirectory()) {
            FileOutputStream out =
                    new FileOutputStream(formattedDate);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
                       MediaScannerConnection.scanFile(context,
                    new String[] { formattedDate }, null,null);

        } else {

        }
        return formattedDate;
    }

//    public static String saveBMPToSDCard(Bitmap bmp, Context context, String prefix) throws Exception {
//        Log.d(TAG, "do_save without insert media");
//        Calendar c = Calendar.getInstance();
//        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
//
//
//        File folder =
//                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/bokeh");
//        String formattedDate =
//                folder.getAbsolutePath() +
//                        "/" + prefix + df.format(c.getTime()) + ".jpg";
//        boolean success = true;
//        if (!folder.exists()) {
//            success = folder.mkdirs();
//        }
//        if (success && folder.isDirectory()) {
//            FileOutputStream out =
//                    new FileOutputStream(formattedDate);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
//            MediaScannerConnection.scanFile(context,
//                    new String[] { formattedDate }, null,null);
//
//        } else {
//
//        }
//
//        return formattedDate;
//
//    }
    public static Bitmap rotateBitmap(Bitmap bitmap, int rotation, boolean isBack) {
        Matrix matrix = new Matrix();

        // Rotate the image back to straight.}
        if (!isBack && rotation % 180 == 0) rotation -= 180;

        matrix.postRotate(rotation);
        if (isBack) {
//            matrix.postScale(1.0f, 1.0f); //640 x 360
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } else {
            // Mirror the image along X axis for front-facing camera image.
            matrix.postScale(-1.0f, 1.0f);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
    }
    public static String saveImageForQ(Bitmap bitmap, @NonNull String prefix, Context context) throws IOException {
        //https://stackoverflow.com/questions/56904485/how-to-save-an-image-in-android-q-using-mediastore

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
        prefix = prefix + df.format(c.getTime()) + ".jpg";

        OutputStream fos;
        File image = null;
        String path;
        ContentResolver resolver = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, prefix + ".jpg");
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            path = imageUri.getPath();
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            image = new File(imagesDir, prefix + ".jpg");
            path = image.getAbsolutePath();
            fos = new FileOutputStream(image);
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

        if (null!=image) {
            MediaScannerConnection.scanFile(context,
                    new String[] { image.getAbsolutePath() }, null,null);
        }
        Objects.requireNonNull(fos).close();
        return path;
    }
    /**
     * A copy of the Android internals  insertImage method, this method populates the
     * meta data with DATE_ADDED and DATE_TAKEN. This fixes a common problem where media
     * that is inserted manually gets saved at the end of the gallery (because date is not populated).
     *
     * @see android.provider.MediaStore.Images.Media # insertImage(ContentResolver, Bitmap, String, String)
     */
//    public static String insertImage(ContentResolver cr,
//                                     Bitmap source,
//                                     String title,
//                                     String description, String fNamePrefix, long timeCapture,
//                                     int orientation) {
//        Log.d(TAG, "do_save with insert media");
//
//        //long timeCapture = System.currentTimeMillis();
//        String mFileName = fNamePrefix + "_IMG_" + timeCapture;
//        File storageDir =
//                new File(Environment.getExternalStorageDirectory() + "/" + Constants.SAVED_DIR);
//        File mFile = new File(storageDir + "/" + mFileName + ".jpg");
//        Log.d(TAG, "Save file name" + mFile.getAbsolutePath());
//        ContentValues values = new ContentValues();
//
//        values.put(MediaStore.Images.Media.TITLE, title);
//        values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
//        values.put(MediaStore.Images.Media.DESCRIPTION, description);
//        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
//        // values.put(MediaStore.Images.Media.ORIENTATION, 90);
//        // Add the date meta data to ensure the image is added at the front of the gallery
//        values.put(MediaStore.Images.Media.DATE_ADDED, timeCapture / 1000);
//        values.put(MediaStore.Images.Media.DATE_TAKEN, timeCapture);
//        //values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, "IMG_" + timeCapture + "_bokeh.jpg");
//        values.put(MediaStore.Images.ImageColumns.DATA, mFile.getAbsolutePath());
//
//        Uri url = null;
//        String stringUrl = null;    /* value to be returned */
//
//        try {
//            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//
//            if (source != null) {
//                OutputStream imageOut = cr.openOutputStream(url);
//                try {
//                    source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
//                    ExifInterface ei = new ExifInterface(mFile.getAbsolutePath());
//
//                    ei.setAttribute(ExifInterface.TAG_ORIENTATION, getOrientationString(orientation));
//                    ei.saveAttributes();
//
//                    int x = 5;
//                } finally {
//                    imageOut.close();
//                }
//
//                long id = ContentUris.parseId(url);
//                // Wait until MINI_KIND thumbnail is generated.
//                Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
//                // This is for backward compatibility.
//                storeThumbnail(cr, miniThumb, id, 50F, 50F, MediaStore.Images.Thumbnails.MICRO_KIND);
//            } else {
//                cr.delete(url, null, null);
//                url = null;
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "insert image to sd failed");
//            e.printStackTrace();
//            if (url != null) {
//                cr.delete(url, null, null);
//                url = null;
//            }
//        }
//
//        if (url != null) {
//            stringUrl = url.toString();
//        }
//
//        return stringUrl;
//    }

    private static String getOrientationString(int orientation) {
        switch (orientation) {

            case 90:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_90);
            case 180:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_180);
            case 270:
                return String.valueOf(ExifInterface.ORIENTATION_ROTATE_270);
            default:
                return String.valueOf(ExifInterface.ORIENTATION_NORMAL);
        }

    }

    /**
     * A copy of the Android internals StoreThumbnail method, it used with the insertImage to
     * populate the android.provider.MediaStore.Images.Media#insertImage with all the correct
     * meta data. The StoreThumbnail method is private so it must be duplicated here.
     *
     * @see android.provider.MediaStore.Images.Media (StoreThumbnail private method)
     */
    private static final Bitmap storeThumbnail(
            ContentResolver cr,
            Bitmap source,
            long id,
            float width,
            float height,
            int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(),
                source.getHeight(), matrix,
                true
        );

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND, kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, (int) id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.getWidth());

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    public static Boolean bmpToFileDebug(Boolean isDebug, Bitmap bmp, String filename) {
        if (!isDebug) return false;
        try (FileOutputStream out =
                     new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + filename)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] bufferToBytes(ByteBuffer buffer) {
        //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static ImageData convert_rgbToNV21(Image image) {
        ByteBuffer rBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer gBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer bBuffer = image.getPlanes()[2].getBuffer();

        byte[] rBytes = bufferToBytes(rBuffer);
        byte[] gBytes = bufferToBytes(gBuffer);
        byte[] bBytes = bufferToBytes(bBuffer);

        int width = image.getWidth();
        int height = image.getHeight();
        int frameSize = width * height;
        int R, G, B, Y, U, V;
        int uvIndex = frameSize;
        byte[] nv21 = new byte[(int) (frameSize * 1.5)];
        for (int i = 0; i < width * height; i++) {
            R = rBytes[i];
            G = gBytes[i];
            B = bBytes[i];
            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

            nv21[i] = (byte) Y;
            if ((i + 1) % 4 == 0) {
                nv21[uvIndex++] = (byte) (U);
                nv21[uvIndex++] = (byte) (V);
            }

        }
        // ImageData(byte[] mData, int width, int height, int imageFormat, int rotation)

        return new ImageData(nv21, width, height, ExImageFormat.CONVERTED_NV21, -1);
    }
    public static byte[] convert_RgbaToNV21(int[] argb, int width, int height) {
        final int frameSize = width * height;
        byte[] nv21 = new byte[frameSize*3/2];
        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                nv21[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    nv21[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    nv21[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
        return nv21;
    }


    public static ImageData convertToImageData(Image image, int requestedFormat) {
        switch (requestedFormat) { //better change to switch_case with image instead of requested
            // format.
            case ExImageFormat.CONVERTED_NV21:
                Bitmap tmp = decodeImageJPEGToBMP(image);
                int[] intArray = bitmapToInt(tmp);

//                long start = System.currentTimeMillis();
//
                byte[] nv21 = convert_RgbaToNV21(intArray, tmp.getWidth(), tmp.getHeight());
//
//                Log.d(TAG, "convert_RgbaToNV21 duration " + (System.currentTimeMillis()-start));
                return new ImageData(nv21, image.getWidth(), image.getHeight(),
                        ExImageFormat.NV21, -1);
            case ExImageFormat.JPEG:
                Bitmap bitmap = decodeImageJPEGToBMP(image);
                byte[] bytes = bitmapToBytes(bitmap);
                return new ImageData(bytes, image.getWidth(), image.getHeight(),
                        ExImageFormat.RGBA, -1);
               /* ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return new ImageData(bytes, image.getWidth(), image.getHeight(),ImageFormat.JPEG, -1);*/
            case ExImageFormat.NV21:
                return convert_YUV420888ToNV(image, requestedFormat);
            default:
                return null;
        }
    }

    public static byte[] bitmapToBytes(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
    }

    public static int[] bitmapToInt(Bitmap bitmap){
        int x = bitmap.getWidth();
        int y = bitmap.getHeight();
        int[] intArray = new int[x * y];
        bitmap.getPixels(intArray, 0, x, 0, 0, x, y);
        return intArray;
    }

    /**
     * Return an byte array of I420 image
     * @param image an Android YUV_420_888 image which U/V pixel stride may be larger than the
     *              size of a single pixel
     * @return I420 byte array which U/V pixel stride is always 1.
     */
    private static byte[] convert_YUV_420_888_To_I420(Image image) {

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[imageWidth * imageHeight *
                ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        int offset = 0;

        for (int plane = 0; plane < planes.length; ++plane) {
            final ByteBuffer buffer = planes[plane].getBuffer();
            final int rowStride = planes[plane].getRowStride();
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed.
            final int pixelStride = planes[plane].getPixelStride();
            final int planeWidth = (plane == 0) ? imageWidth : imageWidth / 2;
            final int planeHeight = (plane == 0) ? imageHeight : imageHeight / 2;
            if (pixelStride == 1 && rowStride == planeWidth) {
                // Copy whole plane from buffer into |data| at once.
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride.
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; ++row) {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; ++col) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data.
                // See http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; ++col) {
                    data[offset++] = rowData[col * pixelStride];
                }
            }
        }

        return data;
    }

    /**
     * Input: YUV420888 (which is captured from Android Camera2)
     * Output: bytes array of NV21 format (YYYYYYYY VUVU)
     * size: WxHx1.5, UV - interleaved
     */

    public static ImageData convert_YUV420888ToNV(Image image, int dstNVFormat) {

        if (image == null) return null;
        if (image.getFormat() != ImageFormat.YUV_420_888) return null;

        int width = image.getWidth();
        int height = image.getHeight();
        int yByteLen = width * height;
        int swapped = (ExImageFormat.NV12 == dstNVFormat) ? 0 : 1;
        ByteBuffer yBuffer;
        ByteBuffer uBuffer;
        ByteBuffer vBuffer;
        try {
            yBuffer = image.getPlanes()[0].getBuffer();
            uBuffer = image.getPlanes()[1 + swapped].getBuffer();
            vBuffer = image.getPlanes()[2 - swapped].getBuffer();
        } catch (Exception e) {
            Log.e(TAG, "Fail to get buffers from image.");
            e.printStackTrace();
            return null;
        }

        int uPixelStride = image.getPlanes()[1].getPixelStride();
        int uvPixelStride = image.getPlanes()[2].getPixelStride();
        int uvByteLen = width * height / 4 * uvPixelStride;

        byte[] NV = new byte[yByteLen * 3 / 2];
        yBuffer.get(NV, 0, yByteLen);

        int currentPos = yByteLen;
        if (yByteLen + uvByteLen / uvPixelStride * 2 != NV.length) {
            Log.d(TAG, "length not matched");
            return null;
        }


        for (int i = 0; i < uvByteLen / uvPixelStride; i++) {
            //UV interleaved
            NV[currentPos] = uBuffer.get(i * uvPixelStride); //U-value
            NV[currentPos + 1] = vBuffer.get(i * uvPixelStride); //V-value
            currentPos += 2;

        }

        ImageData imageData = new ImageData(NV, width, height, dstNVFormat, 0);
       /* Log.d(TAG, String.format("NV21 size (%d,%d), bytes length %d, matched: %s", width,
                height, imageData.getData().length, currentPos==NV.length));*/

        return imageData;
    }

    public static void saveBytesToFile(byte[] data, String path, String extension, int w, int h) throws IOException {

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSSSSS");

        String formattedDate =
                path +
                        "/" + "ce_" + w + "x" + h + "_" + df.format(c.getTime()) + extension;
        boolean success = true;
        File folder = new File(path);

        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success && folder.isDirectory()) {
            FileOutputStream out = new FileOutputStream(formattedDate);
            out.write(data);
            out.close();

        } else {
            // Do something else on failure
        }

    }

    public static byte[] convertBitmapToBytes(Bitmap bitmap) {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
    }

    public static void convert_NV21ToI420(byte[] yuv, int width, int height) {

        long start = System.currentTimeMillis();
       // int[] yuv420 = new int[yuv.length];
        int y_len = width*height;
        int vu_len = y_len / 2;
        int v_len = vu_len / 2;

        //System.arraycopy(nv21, 0, yuv420, 0, y_len);
        int i = 0;
        int k = 0;
        byte[] vu = new byte[vu_len];
        while (i<v_len) {
            k = y_len + (i << 1);
            vu[i] = yuv[k+1]; //u-bytes
            vu[i + v_len ] = yuv[k]; //v-bytes
            i += 1;
        }

        System.arraycopy(vu, 0, yuv, y_len, vu_len);

        long duration = System.currentTimeMillis() - start;
        Log.d(TAG, "nv21Toyuv420 duration: " + duration);
       // return nv21;

    }

//    public static byte[] rs_convert_nv21_To_rgba(byte[] nv21, int width, int height) {
//        return RenderScriptHelper.convertYuvToRgbIntrinsic(mRs, nv21, width, height);
//    }

//    public static byte[] rs_convert_rgba_To_nv21(byte[] rgba, int width, int height) {
//        Bitmap bmp = rgbaBytesToBitmap(rgba, width, height);
//        return RenderScriptHelper.convert_rgba_to_nv21(mRs, bmp);
//    }

    public static byte[] rs_convert_rgba_To_nv21(byte[] rgba, int width, int height) {
        Bitmap bmp = rgbaBytesToBitmap(rgba, width, height);
        byte[] yuv = RenderScriptHelper.convert_rgba_to_i420(mRs, bmp);
        convert_I420_NV21(yuv, width, height);
        return yuv;
    }

    public static void convert_I420_NV21(byte[] yuv, int width, int height){
        int y_len = width*height;
        int vu_len = y_len / 2;
        int u_len = vu_len / 2;
        int i = 0;
        int k = 0;
        byte[] vu = new byte[vu_len];
        while (i<u_len) {
            k = y_len+i;
            vu[i<<1] = yuv[k+u_len]; // v-bytes
            vu[(i<<1)+1] = yuv[k]; //u-bytes
            ++i;
        }
        System.arraycopy(vu, 0, yuv, y_len, vu.length);
    }
}
