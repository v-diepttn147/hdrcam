package com.hdrcam.lowlight;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hdrcam.camera.activity.GLActivity;
import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ExImageFormat;
import com.hdrcam.camera.utils.ImageData;
import com.hdrcam.camera.utils.RawImageData;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class CameraActivity extends GLActivity {
    public static final String TAG = "LowLightSDK";
    private static final int REQUEST_CODE_PERMISSION = 11;

    private String burstTimestamp;

    public CompoundButton switchFrontRearCam;
    public Button saveButt;

    private TextView displayAE;

    private ImageData jpegImage;
    private boolean flagJpeg, takingPicture = false;

    private int minLuminanceThreshold = 60;
    private int maxLuminanceThreshold = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Start CameraActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        checkPermission();
        initCameraEngine();
        initButtons();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            if (!takingPicture) {
                saveButt.setEnabled(false);
                prepareTakePicture();
                takingPicture = true;
            }
        }
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        super.onKeyLongPress(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (!takingPicture) {
                saveButt.setEnabled(false);
                prepareTakePicture();
                takingPicture = true;

            }
        }
        return false;
    }


    private void initCameraEngine() {
        String camMode = "single";
        GLSurfaceView mGlSurfaceView = findViewById(R.id.test_gl_surface_view);
        if (camMode.equals("dual")) {
            setRenderer(camMode, mGlSurfaceView, ExImageFormat.NV21, Constants.saveFormat, true);
        }
        else
            setRenderer(camMode, mGlSurfaceView, ExImageFormat.NV21, Constants.saveFormat, false);

        flagJpeg = false;
    }

    private void getButtonIDs() {
         switchFrontRearCam = findViewById(R.id.switchFrontRearCam);
    }

    private void initButtons() {
        getButtonIDs();

        displayAE = findViewById(R.id.displayAE);

        saveButt = findViewById(R.id.test_doSave);
        saveButt.setOnClickListener((View v) -> {
            saveButt.setEnabled(false);
            prepareTakePicture();
        });

        mMainPreview.createCaptureSession();

        Constants.isAutoexposure = true;

        switchFrontRearCam.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onPause();
            switchFrontRearCamera(!isChecked);
            // Re-assign FrameHandler
            mMainPreview.setFrameHandler(this);
            onResume();
        });

        // Listen to event from preview such as onExposureChanged
        mMainPreview.setFrameHandler(this);
    }

    public void prepareTakePicture() {
        Constants.isAutoexposure = false;
        int autoExpVal = (int) (1000000000L / Constants.mAutoExposureTime);
        int autoISOVal = Constants.mAutoISOValue;
        Log.d("autoExposurePriority", "Auto ISO: " + autoISOVal + " - Auto Exp: 1/" + autoExpVal);

        float refVal = autoISOVal *  (1.f / autoExpVal);
        Log.d("autoExposurePriority", "refVal: " + refVal);

        int corrISO = 100, expIndex;
        for (expIndex = 0; expIndex < Constants.allExpFrac.length; expIndex++) {
            corrISO = (int) (refVal / (1.f / Constants.allExpFrac[expIndex]));
            if (corrISO >= 100) break;
        }
        int corrISOBoundIndex = findNearestValue(corrISO, Constants.allISO);
        Log.d("autoExposurePriority", "Exp: 1/" + Constants.allExpFrac[expIndex] + " - corrISO: " +
                corrISO + " - corrISOBound: " + Constants.allISO[corrISOBoundIndex]);

        Constants.ISOCustomVal = corrISO;
        Constants.expTimeVal = expIndex;
        Constants.ISOVal = corrISOBoundIndex;
        mMainPreview.createCaptureSession();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                takePicture();
            }
        }, 300);
    }

    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                "android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(REQUEST_CODE_PERMISSION);
        }
    }

    private void requestPermissions(int requestCode) {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");
        ActivityCompat.requestPermissions(this, new String[]{
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE"}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showMissingPermissionError();
                    return;
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void showMissingPermissionError(){
        new AlertDialog.Builder(this).setCancelable(false).setMessage(
                "No permission to run app").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                CameraActivity.this.finish();
            }
        }).show();
    }

    @Override
    public void onSingleJPEG(ImageData imageData, int burstIndex) {
        Constants.mainSavedImData = new ImageData(imageData.getData(), imageData.getWidth(),
                imageData.getHeight(), imageData.getFormat(), imageData.getOrientation());
        flagJpeg = true;
        Log.d(TAG,"jpeg image " + imageData.getWidth() + " " + imageData.getHeight());
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBurstStart() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
        SimpleDateFormat tdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        burstTimestamp = df.format(c.getTime());
        Constants.burstTimestamp = burstTimestamp;
        Constants.timeStamp = tdf.format(c.getTime());
    }

    @Override
    public void onBurstComplete() {
        long process_time, start = System.currentTimeMillis();
    }

    public void autoExposureOnPreview(int avgLuminance) {
        Log.d("autoExposureOnPreview", "Luminance: " + avgLuminance);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayAE.setText("Luminance: " + avgLuminance + " (" + minLuminanceThreshold + "-" +
                        maxLuminanceThreshold + ")" + "\nExpusure time: 1/" +
                        Constants.allExpFrac[Constants.expTimeVal] + " s\nISO: 100");
            }
        });
        if (Constants.totalFrames % 3 == 0) {
            if (avgLuminance < minLuminanceThreshold) {
                if (Constants.expTimeVal != 0) {
                    Constants.expTimeVal = Constants.expTimeVal - 1;
                    mMainPreview.createCaptureSession();
                    Log.d("autoExposureOnPreview", "Exposure change from " + Constants.allExpFrac[Constants.expTimeVal + 1] +
                            " to " + Constants.allExpFrac[Constants.expTimeVal]);
                } else Log.d("autoExposureOnPreview", "Exposure level already reach maximum level");
            } else if (avgLuminance > maxLuminanceThreshold) {
                if (Constants.expTimeVal != Constants.allExpFrac.length - 1) {
                    Constants.expTimeVal = Constants.expTimeVal + 1;
                    mMainPreview.createCaptureSession();
                    Log.d("autoExposureOnPreview", "Exposure change from " + Constants.allExpFrac[Constants.expTimeVal - 1] +
                            " to " + Constants.allExpFrac[Constants.expTimeVal]);
                } else Log.d("autoExposureOnPreview", "Exposure level already reach minimum level");
            }
        }
    }

    @Override
    public void onExposureChanged(Long exposureTime, Integer iso) {
        if (exposureTime == null || iso == null) return;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Constants.isAutoexposure){
                    Constants.expTimeVal = findNearestValue((int) (1000000000L/ exposureTime), Constants.allExpFrac);

                    Constants.ISOVal = findNearestValue(iso, Constants.allISO);
                }
            }
        });
    }

    public static int findNearestValue(int value, int[] a) {
        if(value < a[0]) {
            return 0;
        }
        if(value > a[a.length-1]) {
            return a.length - 1;
        }

        int lo = 0;
        int hi = a.length - 1;

        while (lo <= hi) {
            int mid = (hi + lo) / 2;

            if (value < a[mid]) {
                hi = mid - 1;
            } else if (value > a[mid]) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        // lo == hi + 1
        return (a[lo] - value) < (value - a[hi]) ? lo : hi;
    }
}
