package com.hdrcam.camera;

import com.hdrcam.camera.utils.ImageData;

import java.nio.ShortBuffer;

public interface ImageProcessingListener {
    ImageData doSingleImageProcessingForPreview(ImageData imageData);
    ImageData doDualImageProcessingForPreview(ImageData mainImData, ImageData subImData);
    ImageData doSingleImageProcessingForSave(ImageData imageData);
    ImageData doSingleRAWProcessingForSave(ShortBuffer rawShortBuffer, int width, int height);
    ImageData doDualImageProcessingForSave(ImageData mainImData, ImageData subImData);
}
