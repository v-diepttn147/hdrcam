package com.hdrcam.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.widget.Toast;;

import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ImageData;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class ImageUtils {

    public static Bitmap getBitmapFromAsset(Context context, String filePath) {
        InputStream istr = null;
        AssetManager assetManager = context.getAssets();
        try {
            istr = assetManager.open(filePath);
            Bitmap bitmap = BitmapFactory.decodeStream(istr);
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            return bitmap;
        } catch (IOException e) {
            Toast.makeText(context, "Error load image", Toast.LENGTH_LONG).show();
        } finally {
            closeStream(istr);
        }
        return null;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static byte[] bitmapToRGBByteArrayFloat(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer
                .allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4 * 3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] pixelValues = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixelValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int val = pixelValues[pixel++];
                byteBuffer.putFloat((val >> 16 & 0xFF) / 255f);
                byteBuffer.putFloat((val >> 8 & 0xFF) / 255f);
                byteBuffer.putFloat((val & 0xFF) / 255f);
            }
        }
        return byteBuffer.array();
    }

    public static Bitmap rgbByteArrayFloatToBitmap(byte[] byteArray, int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap bitmap = Bitmap.createBitmap(width , height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int r = (int) (byteBuffer.getFloat() * 255f);
            int g = (int) (byteBuffer.getFloat() * 255f);
            int b = (int) (byteBuffer.getFloat() * 255f);

            r = Math.min(Math.max(r, 0), 255);
            g = Math.min(Math.max(g, 0), 255);
            b = Math.min(Math.max(b, 0), 255);

            pixels[i] = 0xff << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
//            pixels[i] = a << 24 | (g & 0xff) << 16 | (b & 0xff) << 8 | (r & 0xff);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static byte[] bitmapToRGBByteArrayInt(Bitmap bitmap) {
        int[] pixelValues = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixelValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        byte[] bytes = new byte[pixelValues.length * 3];

        int pixel = 0;
        int byteIndex = 0;
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int val = pixelValues[pixel++];
                bytes[byteIndex++] = (byte) (val >> 16 & 0xFF);
                bytes[byteIndex++] = (byte) (val >> 8 & 0xFF);
                bytes[byteIndex++] = (byte) (val & 0xFF);
            }
        }
        return bytes;
    }

    public static Bitmap rgbByteArrayIntToBitmap(byte[] byteArray, int width, int height) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.nativeOrder());

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        for (int i = 0; i < width * height; i++) {
            int r = byteBuffer.get();
            int g = byteBuffer.get();
            int b = byteBuffer.get();
            pixels[i] = 0xff << 24 | (r & 0xff) << 16 | (g & 0xff) << 8 | (b & 0xff);
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static byte[] rgb2rgba(byte[] rgb, int width, int height) {
        byte[] rgba = new byte[width * height * 4];
        int j = 0;
        for (int i = 0; i < width * height * 3; i++) {
            rgba[j] = rgb[i];
            j += 1;
            if (i % 3 == 2) {
                rgba[j] = (byte) 255;
                j += 1;
            }
        }
        return rgba;
    }

    public static byte[] rgba2rgb(byte[] rgba, int width, int height) {
        byte[] rgb = new byte[width * height * 3];
        int j = 0;
        for (int i = 0; i < width * height * 4; i++) {
            if (i % 4 == 3) continue;
            rgb[j] = rgba[i];
            j += 1;
        }
        return rgb;
    }

    public static byte[] getBytes(Context context, Uri uri) {
        InputStream istr = null;
        try {
            istr = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = istr.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            istr.close();
            byteBuffer.close();
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(istr);
        }
        return null;
    }

    public static Bitmap readBitmapFromFile(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    public static byte[] readImageBytes(String imagePath) throws IOException {
        Bitmap bitmap = readBitmapFromFile(imagePath);
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
    }

    public static ByteBuffer readYUV(String file_path) throws IOException {
        RandomAccessFile file = new RandomAccessFile(file_path, "r");
        FileChannel channel = file.getChannel();
        ByteBuffer temp = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size()).order(ByteOrder.nativeOrder());
        byte[] byteArray = new byte[temp.remaining()];
        temp.get(byteArray);
        channel.close();
        file.close();
        return temp;
    }

    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
    }

    public static Bitmap byteArrayToBitmap(byte[] byteArray, int imageWidth, int imageHeight) {
        Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        bmp.copyPixelsFromBuffer(buffer);
        return bmp;
    }

    public static Bitmap bitmapFromRaw(byte[] rawImage, int width, int height) {
        byte[] bits = new byte[rawImage.length * 4];
        for(int i = 0; i < rawImage.length; i++)
        {
            bits[i * 4] = bits[i * 4 + 1] = bits[i * 4 + 2] = (byte) ~rawImage[i]; //Invert the source bits
            bits[i * 4 + 3] = (byte) 0xff; // the alpha.
        }

        //Now put these nice RGBA pixels into a Bitmap object
        Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bm.copyPixelsFromBuffer(ByteBuffer.wrap(bits));
        return bm;
    }

    public static void saveJPEG(Context context, Bitmap bmp, String filename) {
        FileOutputStream output = null;
        File jpegFile = getJPEGPath(filename);
        try {
            output = new FileOutputStream(jpegFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, output);
            addMetadata(context, jpegFile, "image/jpeg");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(output);
        }
    }

    public static void saveBitmap(Bitmap bitmap, String savePath) {
        try (FileOutputStream out = new FileOutputStream(savePath)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveJPEG(Context context, byte[] data, String burstTimestamp) {
        FileOutputStream output = null;
        File jpegFile = getJPEGPath(burstTimestamp);
        try {
            output = new FileOutputStream(jpegFile);
            output.write(data);
            addMetadata(context, jpegFile, "image/jpeg");
        } catch (Exception e) {
            Log.e("saveJPEG", "Exception");
            e.printStackTrace();
        } finally {
            closeStream(output);
        }
    }

    public static File getJPEGPath(String name) {
        return new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "JPEG_" + name + ".jpg");
    }

    public static void addMetadata(Context context, File imageFile, String mime_type) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, mime_type); // or image/png
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        MediaScannerConnection.scanFile(context,
                new String[]{imageFile.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
        try {
            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_DATETIME, Constants.timeStamp);
            exif.setAttribute(ExifInterface.TAG_MODEL, "vsmart Joy 3");
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, String.valueOf(2.0f));
            exif.setAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS, String.valueOf(Constants.ISOCustomVal));
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_MODE, String.valueOf(2));
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, String.valueOf(1.f / Constants.allExpFrac[Constants.expTimeVal]));
            exif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveRaw(Context context, ImageData rawImage, CameraCharacteristics cameraCharacteristics,
                               CaptureResult result, String burstTimestamp) {
        Log.d("saveRaw", String.format("raw image saving %d %d %d", rawImage.getFormat(),
                rawImage.getWidth(), rawImage.getHeight()));
        ByteBuffer buffer = ByteBuffer.wrap(rawImage.getData());
        buffer.rewind();

        DngCreator dngCreator = new DngCreator(cameraCharacteristics, result);
        FileOutputStream output = null;
        File rawFile = new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "RAW_" + burstTimestamp + ".dng");
        try {
            output = new FileOutputStream(rawFile);
            dngCreator.writeByteBuffer(output, new Size(rawImage.getWidth(), rawImage.getHeight()), buffer, 0);
            addMetadata(context, rawFile, "image/x-adobe-dng");
        } catch (Exception e) {
            Log.e("saveRaw", "Exception");
            e.printStackTrace();
        } finally {
            closeStream(output);
        }
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
