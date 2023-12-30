package com.hdrcam.camera.capture;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.media.Image;

import com.hdrcam.camera.utils.ImageData;
import com.hdrcam.camera.utils.RawImageData;

/**
 * Created by oleg on 11/2/17.
 */

public interface FrameHandler {
    void onSingleFramePreview(ImageData mainData);
    void onDualFramePreview(ImageData mainData, ImageData subData);

    // Default doSave for YUV
    void doSave(Context context, ImageData mainData, ImageData subData) throws Exception;
    // Custom doSave for YUV
    void doSave(ImageData imageData, String burstTimestamp);
    // Custom doSave for RAW
    void doSave(Image rawImage, CameraCharacteristics cameraCharacteristics, CaptureResult result, String burstTimestamp);

    void onSingleRaw(ImageData image, int index);
    void onSingleRaw(RawImageData image, int index);
    void onSingleJPEG(ImageData imageData, int index);
    void onBurstStart();
    void onBurstComplete();
    void autoExposureOnPreview(int avgLuminance);
    void onExposureChanged(Long exposureTime, Integer iso);
}
