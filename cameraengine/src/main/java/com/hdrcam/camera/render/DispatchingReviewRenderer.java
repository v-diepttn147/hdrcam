package com.hdrcam.camera.render;

import android.opengl.GLSurfaceView;

import com.hdrcam.camera.capture.VideoCameraPreview;
import com.hdrcam.camera.utils.ImageData;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class DispatchingReviewRenderer implements GLSurfaceView.Renderer {
    private PreviewRenderer normalRearCameraRenderer;
    private PreviewRenderer frontCameraRenderer;
    private PreviewRenderer wideRearCameraRenderer;
    private PreviewRenderer currentRenderer;
    private boolean useWide, useRear;

    public DispatchingReviewRenderer(VideoCameraPreview normalRearMainCamPreview, VideoCameraPreview normalRearSubCamPreview,
                                     VideoCameraPreview wideRearMainCamPreview, VideoCameraPreview wideRearSubCamPreview,
                                     VideoCameraPreview frontMainCamPreview, VideoCameraPreview frontSubCamPreview,
                                     int previewFormat, boolean useRear) {
        normalRearCameraRenderer = new PreviewRenderer(normalRearMainCamPreview, normalRearSubCamPreview, previewFormat);
        wideRearCameraRenderer = new PreviewRenderer(wideRearMainCamPreview, wideRearSubCamPreview, previewFormat);
        frontCameraRenderer = new PreviewRenderer(frontMainCamPreview, frontSubCamPreview, previewFormat);
        currentRenderer = useRear ? normalRearCameraRenderer : frontCameraRenderer;
        this.useRear = useRear;
        this.useWide = false;
    }

    public void switchFrontRearRenderer(boolean useRear) {
        this.useRear = useRear;
        if (useRear) {
            currentRenderer = useWide ? wideRearCameraRenderer : normalRearCameraRenderer;
        } else
            currentRenderer = frontCameraRenderer;
    }

    public void switchNormalWideRenderer(boolean useWide) {
        if (useRear) {
            this.useWide = useWide;
            currentRenderer = useWide ? wideRearCameraRenderer : normalRearCameraRenderer;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.currentRenderer.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.currentRenderer.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        this.currentRenderer.onDrawFrame(gl);
    }

    public ImageData getmImageData() {
        return this.currentRenderer.getmImageData();
    }

    public void setPreviewData(ImageData imageData) {
        this.currentRenderer.setPreviewData(imageData);
    }
}
