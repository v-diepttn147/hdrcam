package com.hdrcam.camera.utils;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.util.Size;

//import com.media.camera.preview.BuildConfig;

import java.util.ArrayList;


public class Constants {
    public static final String DUAL_MAIN_CAM = "0";
    public static final String FRONT_CAM = "1";
    public static final String DUAL_SUB_CAM = "2";
    public static int skipFrames = 0;
    public static int totalFrames = 0;
    // CONFIG CAMERA
/*    public static Constants.CAM_MODE camMode = BuildConfig.FLAVOR.equals("dual") ? CAM_MODE.DUAL :
            CAM_MODE.SINGLE;*/
    public static Constants.CAM_MODE camMode = CAM_MODE.SINGLE;
    public static String cameraIDForSingleMode = FRONT_CAM; // only need for SINGLE cam mode.
    // END OF CONFIG

    public static Constants.SAVE_RESULTS saveResult = SAVE_RESULTS.INPROGRESS;
/*    public static final Size DUAL_CAM_PREVIEW_SIZE = new Size(1280,720);
    public static final Size SINGLE_CAM_PREVIEW_SIZE = new Size(1280,720);*/

/*    public static final Size DUAL_RGB_CAM_SAVE_SIZE = new Size(4000,3000);
    public static final Size DUAL_MONO_CAM_SAVE_SIZE = new Size(2560,1920);

    public static final Size SINGLE_CAM_SAVE_SIZE = new Size(4000,3000);*/

    //    public static final Size SINGLE_CAM_SAVE_SIZE = new Size(3840, 2160);
    public enum CAM_MODE {DUAL, SINGLE};

    public static volatile Image monoSavedImage;
    public static volatile Image rgbSavedImage;

    public static volatile Bitmap subSavedBMP;
    public static volatile Bitmap mainSavedBMP;
    public static volatile Bitmap rgbaSavedResultBMP;
    public static volatile Bitmap monoSavedResultBMP;

    public static ImageData mainPreviewImData;
    public static ImageData subPreviewImData;
    public final static String SAVED_DIR = "DCIM/Camera/";
    public synchronized static ImageData getMainSavedImData() {
        return mainSavedImData;
    }

    public synchronized static void  setMainSavedImData(ImageData mainSavedImData) {
        Constants.mainSavedImData = mainSavedImData;
    }

/*    public synchronized static ImageData getSubSavedImData() {
        return subSavedImData;
    }

    public synchronized static void setSubSavedImData(ImageData subSavedImData) {
        Constants.subSavedImData = subSavedImData;
    }*/

    public static String burstTimestamp = "img";
    public static String timeStamp;
    public static ImageData mainSavedImData = new ImageData(null,0,0,0,0);
    public static ImageData rawSavedImDataByte = new ImageData(null,0,0,0,0);
    public static RawImageData rawSavedImData = new RawImageData(null, null, null);
    public static ImageData subSavedImData = new ImageData(null,0,0,0,0);

    public static final  Object imageDataLock = new Object();

    public static volatile Boolean saveIsLocked = false;
    public enum  SAVE_RESULTS {INPROGRESS,SAVED_OK, SAVED_FAILED};

    public static ArrayList<Rect> faceRects = null;
    public static final Object saveLock = new Object();
    public static final Object previewLock = new Object();


    public static Size userDefinedSaveSize = null;
    public static Size userDefinedPreviewSize = null;
    public static boolean saveWithoutCallInsertMedia = false;
    public static boolean autoWhiteBalanceOn = false;

    /**
     * Variables for HDR processing
     */
    public static final boolean isSaveRefFrame = false;
    public static final boolean isSaveRaw = true;
    public static final boolean isProcessHdr = false;
    public static int burstSize = 1;
    public static final int refFrameIndex = 1;

    /**
     * Variables to keep track of auto-exposure value and auto-ISO value
     * image format, etc.
     */
    public static boolean isAutoexposure = false;
    public static Long mAutoExposureTime;
    public static Integer mAutoISOValue;
    public static int saveFormat = ExImageFormat.CONVERTED_NV21;

    /**
     * For saving exposure value, etc
     */
    public static int expTimeVal = 3;
    public static int totalPicsVal = 8;
    public static int ISOVal = 1;
    public static int ISOCustomVal;

    public static final int[] allExpFrac = {3, 6, 12, 25, 30, 50, 60, 100, 125, 200, 500, 800, 1000, 1600};
    public static final int[] allISO = {50, 100, 200, 400, 800, 1600};
    public static final int MaxTotalPics = 20;
}
