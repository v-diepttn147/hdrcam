/*
package com.hdrcam.camera.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.Toast;

import java.util.ArrayList;

public class UIUtils {
    public static Bitmap drawFaceRectsOnBMP(Bitmap bmp) {
        ArrayList<Rect> faceRects;

      */
/*  synchronized (Constants.mLock) {
            faceRects = Constants.faceRects;
        }*//*

        if (faceRects == null || faceRects.size()==0) return bmp;
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(false);
        paint.setStrokeWidth(3);
        Bitmap newBmp = bmp;//.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(newBmp);

        for (int i = 0; i<Constants.faceRects.size();i++) {
            canvas.drawRect(Constants.faceRects.get(i), paint);
        }

        return newBmp;

    }

    public static void showToast(Activity activity, final String text, int gravity) {

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

    public static void showToast(Context context, final String text, int gravity) {
        final Activity activity = getActivity(context);
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

    public static Activity getActivity(Context context) {
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
*/
